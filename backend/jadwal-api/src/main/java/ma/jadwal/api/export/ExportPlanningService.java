package ma.jadwal.api.export;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.export.ExportPlanningPdf;
import ma.jadwal.export.modele.CellulePlanning;
import ma.jadwal.export.modele.DocumentPlanning;
import ma.jadwal.export.modele.GrillePlanning;
import ma.jadwal.export.modele.LignePlanning;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.planning.entite.SemaineSeance;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.service.GrilleConfig;
import ma.jadwal.referentiel.service.GrilleService;

/**
 * Construit le PDF des emplois du temps à partir du référentiel et d'une version de planning.
 * <p>
 * La grille rendue suit strictement la grille horaire de l'établissement (règle A-07 : rien n'est
 * codé en dur) : une ligne par unité de créneau, un bandeau pleine largeur par plage bloquée
 * (déjeuner, récréation), et fusion verticale des séances de plus d'une unité.
 */
@Service
public class ExportPlanningService {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final EtablissementRepository etablissementRepository;
    private final GroupeRepository groupeRepository;
    private final SeanceRepository seanceRepository;
    private final PlanningVersionRepository planningVersionRepository;
    private final GrilleService grilleService;
    private final ExportPlanningPdf moteurPdf = new ExportPlanningPdf();

    public ExportPlanningService(EtablissementRepository etablissementRepository,
                                 GroupeRepository groupeRepository,
                                 SeanceRepository seanceRepository,
                                 PlanningVersionRepository planningVersionRepository,
                                 GrilleService grilleService) {
        this.etablissementRepository = etablissementRepository;
        this.groupeRepository = groupeRepository;
        this.seanceRepository = seanceRepository;
        this.planningVersionRepository = planningVersionRepository;
        this.grilleService = grilleService;
    }

    /**
     * Génère le PDF.
     *
     * @param groupeId  limite l'export à un groupe ; {@code null} = tous les groupes de l'établissement
     * @param versionId version de planning ; {@code null} = version active
     * @param semaine   {@code TOUTES} (défaut), {@code A} ou {@code B} pour les établissements à quinzaine
     */
    @Transactional(readOnly = true)
    public byte[] exporter(Long etablissementId, Long groupeId, Long versionId, SemaineSeance semaine) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable."));
        PlanningVersion version = resoudreVersion(etablissementId, versionId);
        GrilleConfig grille = grilleService.lireGrille(etablissementId);
        if (grille == null) {
            throw new ConflitException("Aucune grille horaire : configurez-la avant d'exporter.");
        }

        List<Groupe> groupes = groupesAExporter(etablissementId, groupeId);
        if (groupes.isEmpty()) {
            throw new ConflitException("Aucun groupe à exporter.");
        }

        // Les séances d'un sous-groupe apparaissent dans la grille de sa classe : c'est ce que
        // consultent les élèves, et c'est cohérent avec l'écran Planning.
        List<Seance> seances = seanceRepository.findByVersionId(version.getId());
        Map<Long, List<Seance>> parGroupe = new LinkedHashMap<>();
        for (Seance seance : seances) {
            if (semaine != SemaineSeance.TOUTES && seance.getSemaine() != SemaineSeance.TOUTES
                    && seance.getSemaine() != semaine) {
                continue;
            }
            Groupe porteur = seance.getGroupe();
            Long cle = porteur.getParent() == null ? porteur.getId() : porteur.getParent().getId();
            parGroupe.computeIfAbsent(cle, k -> new ArrayList<>()).add(seance);
        }

        List<GrillePlanning> grilles = new ArrayList<>();
        for (Groupe groupe : groupes) {
            grilles.add(construireGrille(groupe, parGroupe.getOrDefault(groupe.getId(), List.of()), grille));
        }

        DocumentPlanning document = new DocumentPlanning(
                etablissement.getNom(),
                "Maternelle – Primaire – Collège - Lycée",
                anneeScolaire(),
                grilles);
        return moteurPdf.generer(document);
    }

    /** Nom de fichier proposé au téléchargement. */
    public String nomFichier(Long groupeId, Long etablissementId) {
        if (groupeId == null) {
            return "emplois-du-temps.pdf";
        }
        return groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                .map(g -> "emploi-du-temps-" + assainir(g.getLibelle()) + ".pdf")
                .orElse("emploi-du-temps.pdf");
    }

    // ------------------------------------------------------------------
    // Construction d'une grille
    // ------------------------------------------------------------------

    private GrillePlanning construireGrille(Groupe groupe, List<Seance> seances, GrilleConfig grille) {
        List<Jour> jours = joursActifs(grille);
        List<String> entetes = jours.stream().map(ExportPlanningService::libelleJour).toList();

        // Index (jour, unité) -> séances qui DÉMARRENT là, et unités couvertes par une fusion.
        // Plusieurs séances peuvent partager une case : les sous-groupes dédoublés sont
        // simultanés par construction (B-04).
        Map<String, List<Seance>> departs = new LinkedHashMap<>();
        boolean[][] couvert = new boolean[jours.size()][grille.unitesParJour() + 1];
        for (Seance seance : seances) {
            if (seance.getCreneau() == null) {
                continue;
            }
            int colonne = jours.indexOf(seance.getCreneau().getJour());
            if (colonne < 0) {
                continue;
            }
            int debut = seance.getCreneau().getIndexDebut();
            departs.computeIfAbsent(colonne + ":" + debut, cle -> new ArrayList<>()).add(seance);
            for (int u = debut + 1; u < debut + seance.getDureeUnites() && u < couvert[colonne].length; u++) {
                couvert[colonne][u] = true;
            }
        }

        Map<Integer, GrilleConfig.PlageBloquee> plages = new LinkedHashMap<>();
        for (GrilleConfig.PlageBloquee plage : grille.plagesBloquees()) {
            plages.put(plage.indexDebut(), plage);
        }

        List<LignePlanning> lignes = new ArrayList<>();
        int unite = 0;
        while (unite < grille.unitesParJour()) {
            GrilleConfig.PlageBloquee plage = plages.get(unite);
            if (plage != null) {
                lignes.add(LignePlanning.bandeau(libellePlage(plage, grille)));
                unite += Math.max(1, plage.dureeUnites());
                continue;
            }
            List<CellulePlanning> cellules = new ArrayList<>();
            for (int colonne = 0; colonne < jours.size(); colonne++) {
                if (couvert[colonne][unite]) {
                    cellules.add(null); // case absorbée par la fusion verticale au-dessus
                    continue;
                }
                List<Seance> simultanees = departs.get(colonne + ":" + unite);
                cellules.add(simultanees == null || simultanees.isEmpty()
                        ? CellulePlanning.vide()
                        : versCellule(simultanees));
            }
            lignes.add(LignePlanning.horaire(libelleHoraire(grille, unite), cellules));
            unite++;
        }

        return new GrillePlanning(intitule(groupe), entetes, lignes);
    }

    /**
     * Construit la case. Plusieurs séances simultanées (classe dédoublée, A-04/B-04) sont fusionnées
     * dans une seule case, chaque sous-groupe sur sa ligne — comme sur un emploi du temps papier.
     */
    private CellulePlanning versCellule(List<Seance> simultanees) {
        Seance premiere = simultanees.get(0);
        boolean memeMatiere = simultanees.stream()
                .allMatch(s -> s.getMatiere().getId().equals(premiere.getMatiere().getId()));
        String matiere = memeMatiere
                ? premiere.getMatiere().getLibelle()
                : simultanees.stream().map(s -> s.getMatiere().getLibelle()).distinct()
                        .collect(java.util.stream.Collectors.joining(" / "));

        List<String> lignes = new ArrayList<>();
        for (Seance seance : simultanees) {
            StringBuilder ligne = new StringBuilder();
            if (seance.getGroupe().getParent() != null) {
                // Demi-groupe : on précise lequel.
                ligne.append(seance.getGroupe().getLibelle()).append(" — ");
            }
            if (!memeMatiere) {
                ligne.append(seance.getMatiere().getLibelle()).append(" · ");
            }
            if (seance.getEnseignant() != null) {
                ligne.append(seance.getEnseignant().getNomComplet());
            }
            if (seance.getSalle() != null) {
                if (ligne.length() > 0 && ligne.charAt(ligne.length() - 1) != ' ') {
                    ligne.append(" · ");
                }
                ligne.append(seance.getSalle().getNom());
            }
            if (seance.getSemaine() != SemaineSeance.TOUTES) {
                ligne.append(" (sem. ").append(seance.getSemaine().name()).append(')');
            }
            lignes.add(ligne.toString());
        }

        return new CellulePlanning(matiere, String.join("\n", lignes),
                premiere.getDureeUnites(), premiere.getMatiere().getCouleur());
    }

    // ------------------------------------------------------------------
    // Libellés
    // ------------------------------------------------------------------

    /** « 09:00 - 09:30 », calculé depuis l'heure de début et la durée d'unité de l'établissement. */
    private String libelleHoraire(GrilleConfig grille, int unite) {
        LocalTime debut = heureDebut(grille).plusMinutes((long) unite * grille.dureeUniteMinutes());
        LocalTime fin = debut.plusMinutes(grille.dureeUniteMinutes());
        return HEURE.format(debut) + " - " + HEURE.format(fin);
    }

    private String libellePlage(GrilleConfig.PlageBloquee plage, GrilleConfig grille) {
        int minutes = plage.dureeUnites() * grille.dureeUniteMinutes();
        String duree = minutes % 60 == 0 ? (minutes / 60) + "H" : (minutes / 60) + "H" + (minutes % 60);
        return libelleTypePlage(plage.type()) + " — " + duree;
    }

    /** Les types sont stockés sans accent ; on les restitue correctement à l'impression. */
    static String libelleTypePlage(String type) {
        if (type == null || type.isBlank()) {
            return "PAUSE";
        }
        return switch (type.toUpperCase()) {
            case "DEJEUNER", "DÉJEUNER" -> "DÉJEUNER";
            case "RECREATION", "RÉCRÉATION" -> "RÉCRÉATION";
            default -> type.toUpperCase();
        };
    }

    private LocalTime heureDebut(GrilleConfig grille) {
        try {
            return LocalTime.parse(grille.heureDebut());
        } catch (Exception e) {
            return LocalTime.of(8, 0);
        }
    }

    private String intitule(Groupe groupe) {
        return "Classe : " + groupe.getLibelle();
    }

    /** Année scolaire marocaine : bascule en septembre. */
    static String anneeScolaire() {
        LocalDate aujourdhui = LocalDate.now();
        int debut = aujourdhui.getMonthValue() >= 9 ? aujourdhui.getYear() : aujourdhui.getYear() - 1;
        return debut + "/" + (debut + 1);
    }

    static String libelleJour(Jour jour) {
        String nom = jour.name().toLowerCase();
        return Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
    }

    private static String assainir(String libelle) {
        return libelle == null ? "groupe" : libelle.replaceAll("[^A-Za-z0-9._-]+", "-");
    }

    // ------------------------------------------------------------------
    // Sélection
    // ------------------------------------------------------------------

    private List<Jour> joursActifs(GrilleConfig grille) {
        List<Jour> jours = new ArrayList<>();
        for (String libelle : grille.joursActifs()) {
            try {
                jours.add(Jour.valueOf(libelle.toUpperCase()));
            } catch (IllegalArgumentException ignoree) {
                // Jour inconnu dans la configuration : ignoré.
            }
        }
        jours.sort(Comparator.comparingInt(Enum::ordinal));
        return jours;
    }

    /**
     * Les classes à exporter. Les sous-groupes ne donnent pas de page à part : leurs séances
     * figurent dans la grille de leur classe. Si l'utilisateur demande explicitement un
     * sous-groupe, on exporte la page de sa classe.
     */
    private List<Groupe> groupesAExporter(Long etablissementId, Long groupeId) {
        if (groupeId != null) {
            Groupe groupe = groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + groupeId));
            return List.of(groupe.getParent() == null ? groupe : groupe.getParent());
        }
        return groupeRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId).stream()
                .filter(g -> g.getParent() == null)
                .toList();
    }

    private PlanningVersion resoudreVersion(Long etablissementId, Long versionId) {
        if (versionId != null) {
            return planningVersionRepository.findByIdAndEtablissementId(versionId, etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Version introuvable : " + versionId));
        }
        return planningVersionRepository.findByEtablissementIdAndActiveTrue(etablissementId).stream()
                .findFirst()
                .orElseThrow(() -> new ConflitException(
                        "Aucun planning actif : lancez une génération ou activez une version avant d'exporter."));
    }
}
