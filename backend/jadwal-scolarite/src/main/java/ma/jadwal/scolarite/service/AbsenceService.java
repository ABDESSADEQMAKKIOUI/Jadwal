package ma.jadwal.scolarite.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.scolarite.depot.AbsenceRepository;
import ma.jadwal.scolarite.depot.AgregatAbsences;
import ma.jadwal.scolarite.depot.CompteurAbsencesEleve;
import ma.jadwal.scolarite.depot.EffectifGroupe;
import ma.jadwal.scolarite.depot.EleveRepository;
import ma.jadwal.scolarite.entite.Absence;
import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.entite.TypeAbsence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Règles métier des absences.
 *
 * <p>Trois invariants gouvernent ce service :
 * <ul>
 *   <li>l'{@code etablissementId} vient du JWT et filtre CHAQUE lecture et
 *       écriture, élèves et séances comprises ;</li>
 *   <li>la saisie de la feuille d'appel est idempotente : la même feuille
 *       renvoyée deux fois produit exactement le même état, et un élève retiré
 *       de la feuille voit sa saisie supprimée ;</li>
 *   <li>deux saisies qui se chevauchent (JOURNEE contre MATIN par exemple) ne
 *       coexistent jamais, sinon les statistiques compteraient deux fois.</li>
 * </ul>
 *
 * <p>Aucune donnée personnelle n'est journalisée : seuls des identifiants
 * techniques et des décomptes apparaissent dans les traces.
 */
@Service
public class AbsenceService {

    /** Demi-journées non justifiées déclenchant une alerte, faute de seuil explicite. */
    public static final long SEUIL_ALERTE_PAR_DEFAUT = 4L;

    /**
     * Longueur maximale d'une période de statistiques : un peu plus qu'une année
     * scolaire. Borne la taille de la série renvoyée, qu'une plage de dates
     * fantaisiste pourrait sinon faire exploser.
     */
    public static final int JOURS_MAX_STATISTIQUES = 400;

    private static final Logger journal = LoggerFactory.getLogger(AbsenceService.class);

    private final AbsenceRepository absenceRepository;
    private final EleveRepository eleveRepository;
    private final GroupeRepository groupeRepository;
    private final SeanceRepository seanceRepository;

    public AbsenceService(AbsenceRepository absenceRepository,
                          EleveRepository eleveRepository,
                          GroupeRepository groupeRepository,
                          SeanceRepository seanceRepository) {
        this.absenceRepository = absenceRepository;
        this.eleveRepository = eleveRepository;
        this.groupeRepository = groupeRepository;
        this.seanceRepository = seanceRepository;
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Absence obtenir(Long etablissementId, Long id) {
        return absenceRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Absence introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<Absence> listerParEleve(Long etablissementId, Long eleveId) {
        exigerEleve(etablissementId, eleveId);
        return absenceRepository.findByEtablissementIdAndEleveIdOrderByDateAbsenceDescIdDesc(
                etablissementId, eleveId);
    }

    @Transactional(readOnly = true)
    public List<Absence> listerParEleveEtPeriode(Long etablissementId, Long eleveId, LocalDate debut, LocalDate fin) {
        exigerPeriode(debut, fin);
        exigerEleve(etablissementId, eleveId);
        return absenceRepository
                .findByEtablissementIdAndEleveIdAndDateAbsenceBetweenOrderByDateAbsenceDescIdDesc(
                        etablissementId, eleveId, debut, fin);
    }

    @Transactional(readOnly = true)
    public List<Absence> listerParDate(Long etablissementId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire");
        }
        return absenceRepository.findByEtablissementIdAndDateAbsence(etablissementId, date);
    }

    @Transactional(readOnly = true)
    public List<Absence> listerParPeriode(Long etablissementId, LocalDate debut, LocalDate fin) {
        exigerPeriode(debut, fin);
        return absenceRepository.findByEtablissementIdAndDateAbsenceBetween(etablissementId, debut, fin);
    }

    /**
     * Liste filtrée des saisies d'une période, pour l'écran de suivi.
     * Tous les critères sauf la période sont optionnels : un paramètre nul
     * n'applique aucun filtre.
     *
     * <p>Un filtre de groupe ou de niveau exclut les élèves sans classe, qui
     * n'appartiennent à aucun des deux.
     *
     * @param eleveId   restreint à un élève, contrôlé sur l'établissement
     * @param justifiee {@code null} = justifiées et non justifiées confondues
     */
    @Transactional(readOnly = true)
    public List<Absence> rechercher(Long etablissementId, LocalDate debut, LocalDate fin, Long groupeId,
                                    Long niveauId, Long eleveId, TypeAbsence type, Boolean justifiee) {
        exigerPeriode(debut, fin);
        if (eleveId != null) {
            exigerEleve(etablissementId, eleveId);
        }
        if (groupeId != null) {
            exigerGroupe(etablissementId, groupeId);
        }
        return absenceRepository.rechercher(etablissementId, debut, fin, eleveId, groupeId, niveauId,
                type, justifiee);
    }

    /**
     * Saisies déjà enregistrées pour une feuille d'appel, afin de la préremplir.
     */
    @Transactional(readOnly = true)
    public List<Absence> listerFeuille(Long etablissementId, Long groupeId, LocalDate date,
                                       DemiJournee demiJournee, Long seanceId) {
        DemiJournee contexte = exigerContexteFeuille(etablissementId, groupeId, date, demiJournee);
        List<Long> eleveIds = identifiantsElevesDuGroupe(etablissementId, groupeId);
        if (eleveIds.isEmpty()) {
            return List.of();
        }
        if (seanceId != null) {
            exigerSeance(etablissementId, seanceId);
            return absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIdAndEleveIdIn(
                    etablissementId, date, seanceId, eleveIds);
        }
        return absenceRepository
                .findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(etablissementId, date, eleveIds)
                .stream()
                .filter(absence -> absence.getDemiJournee() == contexte)
                .toList();
    }

    // ------------------------------------------------------------------
    // Saisie
    // ------------------------------------------------------------------

    /**
     * Enregistre la feuille d'appel d'un groupe pour une demi-journée ou une
     * séance. Opération idempotente et faisant foi : les lignes présentes sont
     * créées ou mises à jour, les saisies antérieures qui ne figurent plus dans
     * la feuille sont supprimées.
     *
     * @param groupeId    groupe appelé, obligatoire (contrôlé sur l'établissement)
     * @param demiJournee contexte de la feuille, MATIN par défaut
     * @param seanceId    séance concernée, ou {@code null} pour une demi-journée
     * @param lignes      élèves signalés uniquement ; les autres sont présents
     * @param saisiPar    identifiant de l'utilisateur auteur de la saisie
     * @return les saisies persistées, dans l'ordre de la feuille
     */
    @Transactional
    public List<Absence> enregistrerFeuille(Long etablissementId, Long groupeId, LocalDate date,
                                            DemiJournee demiJournee, Long seanceId,
                                            List<LigneAppel> lignes, Long saisiPar) {
        return appliquerFeuille(etablissementId, groupeId, date, demiJournee, seanceId, lignes, saisiPar)
                .saisies();
    }

    /**
     * Même opération que {@link #enregistrerFeuille}, en rendant compte de son
     * effet : nombre de saisies créées, réécrites et supprimées. L'interface a
     * besoin de ces compteurs pour confirmer une saisie en lot.
     */
    @Transactional
    public ResultatFeuille appliquerFeuille(Long etablissementId, Long groupeId, LocalDate date,
                                            DemiJournee demiJournee, Long seanceId,
                                            List<LigneAppel> lignes, Long saisiPar) {
        DemiJournee contexte = exigerContexteFeuille(etablissementId, groupeId, date, demiJournee);
        Seance seance = seanceId == null ? null : exigerSeance(etablissementId, seanceId);

        Map<Long, Eleve> elevesDuGroupe = new LinkedHashMap<>();
        for (Eleve eleve : eleveRepository.findByEtablissementIdAndGroupeIdOrderByNomAscPrenomAsc(
                etablissementId, groupeId)) {
            elevesDuGroupe.put(eleve.getId(), eleve);
        }

        // Un même élève cité deux fois ne produit qu'une saisie : la dernière ligne gagne.
        Map<Long, LigneAppel> soumises = new LinkedHashMap<>();
        for (LigneAppel ligne : lignes == null ? List.<LigneAppel>of() : lignes) {
            if (ligne == null || ligne.eleveId() == null) {
                throw new IllegalArgumentException("Chaque ligne de la feuille d'appel doit désigner un élève");
            }
            if (!elevesDuGroupe.containsKey(ligne.eleveId())) {
                throw new RessourceIntrouvableException(
                        "Élève introuvable dans ce groupe : " + ligne.eleveId());
            }
            soumises.put(ligne.eleveId(), ligne);
        }

        List<Absence> existantes = elevesDuGroupe.isEmpty()
                ? List.of()
                : seance == null
                        ? absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                                etablissementId, date, elevesDuGroupe.keySet())
                        : absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIdAndEleveIdIn(
                                etablissementId, date, seanceId, elevesDuGroupe.keySet());

        Map<Long, Absence> reprises = new HashMap<>();
        List<Absence> aSupprimer = new ArrayList<>();
        for (Absence existante : existantes) {
            Long eleveId = existante.getEleve().getId();
            boolean memeContexte = seance != null || existante.getDemiJournee() == contexte;
            if (memeContexte && soumises.containsKey(eleveId)) {
                reprises.put(eleveId, existante);
            } else if (seance != null || contexte.chevauche(existante.getDemiJournee())) {
                aSupprimer.add(existante);
            }
        }
        if (!aSupprimer.isEmpty()) {
            absenceRepository.deleteAll(aSupprimer);
            absenceRepository.flush();
        }

        List<Absence> resultat = new ArrayList<>(soumises.size());
        int crees = 0;
        for (Map.Entry<Long, LigneAppel> entree : soumises.entrySet()) {
            Absence absence = reprises.get(entree.getKey());
            if (absence == null) {
                absence = nouvelleAbsence(etablissementId, elevesDuGroupe.get(entree.getKey()), date, seance);
                crees++;
            }
            absence.setDemiJournee(contexte);
            appliquer(absence, entree.getValue(), saisiPar);
            resultat.add(absenceRepository.save(absence));
        }

        journal.info("Feuille d'appel enregistrée : établissement={} groupe={} date={} contexte={} séance={} "
                        + "saisies={} suppressions={}",
                etablissementId, groupeId, date, contexte, seanceId, resultat.size(), aSupprimer.size());
        return new ResultatFeuille(resultat, crees, resultat.size() - crees, aSupprimer.size());
    }

    /**
     * Saisie ou correction d'une absence isolée. Idempotente : réappliquée sur
     * le même élève, la même date et le même contexte, elle met à jour la saisie
     * existante au lieu d'en créer une seconde.
     */
    @Transactional
    public Absence enregistrer(Long etablissementId, LocalDate date, DemiJournee demiJournee, Long seanceId,
                               LigneAppel ligne, Long saisiPar) {
        if (date == null) {
            throw new IllegalArgumentException("La date de l'absence est obligatoire");
        }
        if (ligne == null || ligne.eleveId() == null) {
            throw new IllegalArgumentException("L'élève est obligatoire");
        }
        DemiJournee contexte = demiJournee == null ? DemiJournee.MATIN : demiJournee;
        Eleve eleve = exigerEleve(etablissementId, ligne.eleveId());
        Seance seance = seanceId == null ? null : exigerSeance(etablissementId, seanceId);

        Absence absence;
        if (seance != null) {
            absence = absenceRepository
                    .findByEtablissementIdAndEleveIdAndDateAbsenceAndSeanceId(
                            etablissementId, eleve.getId(), date, seanceId)
                    .orElseGet(() -> nouvelleAbsence(etablissementId, eleve, date, seance));
        } else {
            List<Absence> existantes = absenceRepository
                    .findByEtablissementIdAndEleveIdAndDateAbsenceAndSeanceIsNull(
                            etablissementId, eleve.getId(), date);
            Optional<Absence> exacte = existantes.stream()
                    .filter(candidate -> candidate.getDemiJournee() == contexte)
                    .findFirst();
            List<Absence> chevauchantes = existantes.stream()
                    .filter(candidate -> candidate.getDemiJournee() != contexte)
                    .filter(candidate -> contexte.chevauche(candidate.getDemiJournee()))
                    .toList();
            if (!chevauchantes.isEmpty()) {
                absenceRepository.deleteAll(chevauchantes);
                absenceRepository.flush();
            }
            absence = exacte.orElseGet(() -> nouvelleAbsence(etablissementId, eleve, date, null));
        }
        absence.setDemiJournee(contexte);
        appliquer(absence, ligne, saisiPar);
        return absenceRepository.save(absence);
    }

    /**
     * Correction partielle d'une saisie : chaque paramètre nul laisse la valeur
     * en place (sémantique PATCH). Un motif vide ou constitué d'espaces efface
     * le motif existant, seul moyen de le retirer sans le confondre avec
     * « ne pas y toucher ».
     */
    @Transactional
    public Absence modifier(Long etablissementId, Long id, Boolean justifiee, TypeAbsence type, String motif) {
        Absence absence = obtenir(etablissementId, id);
        if (justifiee != null) {
            absence.setJustifiee(justifiee);
        }
        if (type != null) {
            absence.setType(type);
        }
        if (motif != null) {
            absence.setMotif(motifValide(motif));
        }
        return absenceRepository.save(absence);
    }

    @Transactional
    public Absence justifier(Long etablissementId, Long id, boolean justifiee, String motif) {
        Absence absence = obtenir(etablissementId, id);
        absence.setJustifiee(justifiee);
        absence.setMotif(motifValide(motif));
        return absenceRepository.save(absence);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        absenceRepository.delete(obtenir(etablissementId, id));
    }

    // ------------------------------------------------------------------
    // Statistiques et alertes
    // ------------------------------------------------------------------

    /**
     * Agrégats d'absentéisme sur une période.
     *
     * <p>Le taux rapporte les demi-journées d'absence (type ABSENCE seulement,
     * une absence JOURNEE valant deux demi-journées) au volume théoriquement dû :
     * élèves INSCRIT × jours ouvrables × 2. Les jours ouvrables sont les jours de
     * la période hors dimanche, la semaine scolaire marocaine allant du lundi au
     * samedi. Retards et exclusions sont comptés à part, jamais dans le taux.
     */
    @Transactional(readOnly = true)
    public StatistiquesAbsences statistiques(Long etablissementId, LocalDate debut, LocalDate fin) {
        exigerPeriode(debut, fin);
        long elevesInscrits = eleveRepository.countByEtablissementIdAndStatut(etablissementId, StatutEleve.INSCRIT);
        long totalAbsences = absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                etablissementId, debut, fin, TypeAbsence.ABSENCE);
        long journeesCompletes = absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndDemiJournee(
                etablissementId, debut, fin, TypeAbsence.ABSENCE, DemiJournee.JOURNEE);
        long justifiees = absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndJustifiee(
                etablissementId, debut, fin, TypeAbsence.ABSENCE, true);
        long retards = absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                etablissementId, debut, fin, TypeAbsence.RETARD);
        long exclusions = absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                etablissementId, debut, fin, TypeAbsence.EXCLUSION);

        long joursOuvrables = joursOuvrables(debut, fin);
        long demiJourneesAbsence = totalAbsences + journeesCompletes;
        long demiJourneesDues = elevesInscrits * joursOuvrables * 2L;
        double taux = demiJourneesDues == 0L
                ? 0.0
                : arrondirAuCentieme(100.0 * demiJourneesAbsence / demiJourneesDues);

        return new StatistiquesAbsences(debut, fin, elevesInscrits, joursOuvrables, totalAbsences,
                justifiees, totalAbsences - justifiees, retards, exclusions,
                demiJourneesAbsence, demiJourneesDues, taux);
    }

    /**
     * Synthèse d'absentéisme d'une période : totaux, série temporelle à la
     * granularité demandée et répartition par classe, en un seul agrégat SQL.
     *
     * <p><strong>Formule du taux d'absentéisme</strong> — les demi-journées
     * d'absence constatées sont rapportées au volume de présence dû :
     *
     * <pre>
     * taux = 100 × demi-journées d'absence ÷ (effectif × jours ouvrables × 2)
     * </pre>
     *
     * <ul>
     *   <li><em>demi-journées d'absence</em> : saisies de type ABSENCE
     *       exclusivement, une absence JOURNEE valant deux demi-journées. Les
     *       retards et les exclusions sont dénombrés à part et n'entrent jamais
     *       au numérateur ;</li>
     *   <li><em>effectif</em> : élèves de statut INSCRIT, restreints au groupe ou
     *       au niveau demandé. Pour la ligne d'une classe, c'est le nombre réel
     *       d'élèves rattachés et non la capacité déclarée du groupe : c'est le
     *       seul dénominateur homogène au numérateur ;</li>
     *   <li><em>jours ouvrables</em> : tous les jours de la période hors dimanche
     *       (semaine scolaire marocaine lundi → samedi) ; le facteur 2 traduit les
     *       deux demi-journées de chaque jour de classe ;</li>
     *   <li>dénominateur nul — aucun élève inscrit ou aucun jour ouvrable — le
     *       taux vaut 0 plutôt que d'être indéfini.</li>
     * </ul>
     *
     * <p>Le taux est un pourcentage arrondi au dixième, tel que l'API le publie.
     * {@link #statistiques} garde son arrondi au centième pour les usages
     * internes déjà en place.
     *
     * <p>Les séries couvrent toute la période, y compris les regroupements sans
     * aucune saisie, pour qu'un graphique n'escamote pas les jours à zéro. En
     * granularité {@code JOUR} les dimanches sont omis, sauf si une saisie y a
     * malgré tout été enregistrée.
     *
     * @param periode granularité des séries, {@code JOUR} par défaut
     * @throws IllegalArgumentException si la période dépasse
     *                                  {@link #JOURS_MAX_STATISTIQUES} jours
     */
    @Transactional(readOnly = true)
    public SyntheseAbsences synthese(Long etablissementId, PeriodeStatistiques periode, LocalDate debut,
                                     LocalDate fin, Long groupeId, Long niveauId) {
        exigerPeriode(debut, fin);
        PeriodeStatistiques granularite = periode == null ? PeriodeStatistiques.JOUR : periode;
        if (ChronoUnit.DAYS.between(debut, fin) + 1L > JOURS_MAX_STATISTIQUES) {
            throw new IllegalArgumentException("La période demandée dépasse " + JOURS_MAX_STATISTIQUES
                    + " jours : restreignez les dates de début et de fin");
        }
        if (groupeId != null) {
            exigerGroupe(etablissementId, groupeId);
        }

        long joursOuvrables = joursOuvrables(debut, fin);
        long effectif = eleveRepository.compterParStatut(etablissementId, StatutEleve.INSCRIT, groupeId, niveauId);

        // Série chronologique : la clé d'un regroupement se trie dans l'ordre du temps.
        Map<String, long[]> pointsParCle = new TreeMap<>();
        Map<String, String> libellesParCle = new HashMap<>();
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            if (granularite == PeriodeStatistiques.JOUR && jour.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            inscrireRegroupement(granularite, jour, pointsParCle, libellesParCle);
        }

        long absences = 0L;
        long justifiees = 0L;
        long retards = 0L;
        long exclusions = 0L;
        long demiJourneesAbsence = 0L;
        Map<Long, long[]> cumulsParGroupe = new HashMap<>();

        for (AgregatAbsences agregat : absenceRepository.agreger(etablissementId, debut, fin, groupeId, niveauId)) {
            long nombre = agregat.nombreOuZero();
            long[] point = pointsParCle.get(
                    inscrireRegroupement(granularite, agregat.date(), pointsParCle, libellesParCle));
            switch (agregat.type()) {
                case ABSENCE -> {
                    absences += nombre;
                    demiJourneesAbsence += agregat.demiJournees();
                    if (agregat.estJustifiee()) {
                        justifiees += nombre;
                    }
                    point[0] += nombre;
                    if (agregat.groupeId() != null) {
                        long[] cumul = cumulsParGroupe.computeIfAbsent(agregat.groupeId(), id -> new long[2]);
                        cumul[0] += nombre;
                        cumul[1] += agregat.demiJournees();
                    }
                }
                case RETARD -> {
                    retards += nombre;
                    point[1] += nombre;
                }
                case EXCLUSION -> exclusions += nombre;
            }
        }

        long demiJourneesDues = effectif * joursOuvrables * 2L;
        StatistiquesAbsences totaux = new StatistiquesAbsences(debut, fin, effectif, joursOuvrables, absences,
                justifiees, absences - justifiees, retards, exclusions, demiJourneesAbsence, demiJourneesDues,
                tauxAbsenteisme(demiJourneesAbsence, demiJourneesDues));

        List<SerieAbsences> series = new ArrayList<>(pointsParCle.size());
        pointsParCle.forEach((cle, point) ->
                series.add(new SerieAbsences(cle, libellesParCle.get(cle), point[0], point[1])));

        List<AbsencesParGroupe> parGroupe = new ArrayList<>();
        for (EffectifGroupe groupe : eleveRepository.effectifsParGroupe(
                etablissementId, StatutEleve.INSCRIT, groupeId, niveauId)) {
            long[] cumul = cumulsParGroupe.get(groupe.groupeId());
            long absencesGroupe = cumul == null ? 0L : cumul[0];
            long effectifGroupe = groupe.effectifOuZero();
            if (effectifGroupe == 0L && absencesGroupe == 0L) {
                // Classe sans élève inscrit ni saisie : rien à en dire.
                continue;
            }
            parGroupe.add(new AbsencesParGroupe(groupe.groupeId(), groupe.groupeLibelle(), effectifGroupe,
                    absencesGroupe,
                    tauxAbsenteisme(cumul == null ? 0L : cumul[1], effectifGroupe * joursOuvrables * 2L)));
        }

        return new SyntheseAbsences(granularite, debut, fin, totaux, List.copyOf(series), List.copyOf(parGroupe));
    }

    /**
     * Élèves à convoquer : ceux dont les absences non justifiées atteignent le
     * seuil sur la période. Seuil nul ou négatif = {@link #SEUIL_ALERTE_PAR_DEFAUT}.
     */
    @Transactional(readOnly = true)
    public List<AlerteAbsenteisme> alertes(Long etablissementId, LocalDate debut, LocalDate fin, long seuil) {
        exigerPeriode(debut, fin);
        long seuilEffectif = seuil <= 0L ? SEUIL_ALERTE_PAR_DEFAUT : seuil;
        List<CompteurAbsencesEleve> compteurs = absenceRepository.compterNonJustifieesParEleve(
                etablissementId, TypeAbsence.ABSENCE, debut, fin, seuilEffectif);
        if (compteurs.isEmpty()) {
            return List.of();
        }
        List<Long> eleveIds = compteurs.stream().map(CompteurAbsencesEleve::eleveId).toList();
        Map<Long, Eleve> eleves = new HashMap<>();
        for (Eleve eleve : eleveRepository.findByEtablissementIdAndIdInOrderByNomAscPrenomAsc(
                etablissementId, eleveIds)) {
            eleves.put(eleve.getId(), eleve);
        }
        List<AlerteAbsenteisme> alertes = new ArrayList<>(compteurs.size());
        for (CompteurAbsencesEleve compteur : compteurs) {
            Eleve eleve = eleves.get(compteur.eleveId());
            if (eleve == null) {
                // Filet de sécurité : rien qui n'appartienne pas à l'établissement ne sort d'ici.
                continue;
            }
            alertes.add(new AlerteAbsenteisme(
                    eleve.getId(),
                    eleve.getCodeMassar(),
                    eleve.getNom(),
                    eleve.getPrenom(),
                    eleve.getGroupe() == null ? null : eleve.getGroupe().getId(),
                    eleve.getGroupe() == null ? null : eleve.getGroupe().getLibelle(),
                    compteur.nombre() == null ? 0L : compteur.nombre()));
        }
        return alertes;
    }

    @Transactional(readOnly = true)
    public long compterAbsencesEleve(Long etablissementId, Long eleveId, LocalDate debut, LocalDate fin) {
        exigerPeriode(debut, fin);
        exigerEleve(etablissementId, eleveId);
        return absenceRepository.countByEtablissementIdAndEleveIdAndDateAbsenceBetweenAndType(
                etablissementId, eleveId, debut, fin, TypeAbsence.ABSENCE);
    }

    @Transactional(readOnly = true)
    public long compterDuJour(Long etablissementId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire");
        }
        return absenceRepository.countByEtablissementIdAndDateAbsence(etablissementId, date);
    }

    /**
     * Jours de classe de la période : tous les jours hors dimanche.
     */
    static long joursOuvrables(LocalDate debut, LocalDate fin) {
        long jours = 0L;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            if (jour.getDayOfWeek() != DayOfWeek.SUNDAY) {
                jours++;
            }
        }
        return jours;
    }

    static double arrondirAuCentieme(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }

    static double arrondirAuDixieme(double valeur) {
        return Math.round(valeur * 10.0) / 10.0;
    }

    /**
     * Taux publié par l'API : pourcentage arrondi au dixième, 0 si le
     * dénominateur est nul (aucun élève inscrit ou aucun jour ouvrable).
     */
    static double tauxAbsenteisme(long demiJourneesAbsence, long demiJourneesDues) {
        return demiJourneesDues <= 0L
                ? 0.0
                : arrondirAuDixieme(100.0 * demiJourneesAbsence / demiJourneesDues);
    }

    /**
     * Crée si besoin le point de série du regroupement contenant ce jour et
     * renvoie sa clé.
     */
    private static String inscrireRegroupement(PeriodeStatistiques granularite, LocalDate jour,
                                               Map<String, long[]> pointsParCle,
                                               Map<String, String> libellesParCle) {
        String cle = granularite.cle(jour);
        if (pointsParCle.putIfAbsent(cle, new long[2]) == null) {
            libellesParCle.put(cle, granularite.libelle(jour));
        }
        return cle;
    }

    // ------------------------------------------------------------------
    // Outillage interne
    // ------------------------------------------------------------------

    private Absence nouvelleAbsence(Long etablissementId, Eleve eleve, LocalDate date, Seance seance) {
        Absence absence = new Absence();
        absence.setEtablissementId(etablissementId);
        absence.setEleve(eleve);
        absence.setDateAbsence(date);
        absence.setSeance(seance);
        return absence;
    }

    private void appliquer(Absence absence, LigneAppel ligne, Long saisiPar) {
        absence.setType(ligne.type() == null ? TypeAbsence.ABSENCE : ligne.type());
        absence.setJustifiee(ligne.justifiee());
        absence.setMotif(motifValide(ligne.motif()));
        absence.setSaisiePar(saisiPar);
    }

    private DemiJournee exigerContexteFeuille(Long etablissementId, Long groupeId, LocalDate date,
                                              DemiJournee demiJournee) {
        if (date == null) {
            throw new IllegalArgumentException("La date de la feuille d'appel est obligatoire");
        }
        if (groupeId == null) {
            throw new IllegalArgumentException("Le groupe de la feuille d'appel est obligatoire");
        }
        exigerGroupe(etablissementId, groupeId);
        return demiJournee == null ? DemiJournee.MATIN : demiJournee;
    }

    private List<Long> identifiantsElevesDuGroupe(Long etablissementId, Long groupeId) {
        return eleveRepository.findByEtablissementIdAndGroupeIdOrderByNomAscPrenomAsc(etablissementId, groupeId)
                .stream()
                .map(Eleve::getId)
                .toList();
    }

    /** Chargement filtré par locataire : jamais l'élève d'un autre établissement. */
    private Eleve exigerEleve(Long etablissementId, Long eleveId) {
        if (eleveId == null) {
            throw new IllegalArgumentException("L'élève est obligatoire");
        }
        return eleveRepository.findByIdAndEtablissementId(eleveId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Élève introuvable : " + eleveId));
    }

    /** Chargement filtré par locataire : jamais le groupe d'un autre établissement. */
    private void exigerGroupe(Long etablissementId, Long groupeId) {
        if (groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId).isEmpty()) {
            throw new RessourceIntrouvableException("Groupe introuvable : " + groupeId);
        }
    }

    /** Chargement filtré par locataire : jamais la séance d'un autre établissement. */
    private Seance exigerSeance(Long etablissementId, Long seanceId) {
        return seanceRepository.findByIdAndEtablissementId(seanceId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Séance introuvable : " + seanceId));
    }

    private static void exigerPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires");
        }
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
    }

    private static String motifValide(String motif) {
        if (motif == null || motif.isBlank()) {
            return null;
        }
        String propre = motif.trim();
        if (propre.length() > 200) {
            throw new IllegalArgumentException("Le motif dépasse 200 caractères");
        }
        return propre;
    }
}
