package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.service.ModuleService;
import ma.jadwal.api.dto.AbsenceReponse;
import ma.jadwal.api.dto.AlerteAbsenceReponse;
import ma.jadwal.api.dto.FeuilleAppelReponse;
import ma.jadwal.api.dto.LigneAppelRequete;
import ma.jadwal.api.dto.MappeursAbsence;
import ma.jadwal.api.dto.ModificationAbsenceRequete;
import ma.jadwal.api.dto.ResultatFeuilleReponse;
import ma.jadwal.api.dto.SaisieFeuilleRequete;
import ma.jadwal.api.dto.StatistiquesAbsencesReponse;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.service.GroupeService;
import ma.jadwal.scolarite.entite.Absence;
import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.TypeAbsence;
import ma.jadwal.scolarite.service.AbsenceService;
import ma.jadwal.scolarite.service.EleveService;
import ma.jadwal.scolarite.service.LigneAppel;
import ma.jadwal.scolarite.service.PeriodeStatistiques;
import ma.jadwal.scolarite.service.ResultatFeuille;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Vie scolaire : saisie des absences, feuille d'appel, statistiques et alertes.
 *
 * <p>Trois règles gouvernent chaque méthode de ce contrôleur :
 * <ul>
 *   <li>l'{@code etablissementId} vient EXCLUSIVEMENT du JWT, via
 *       {@link ContexteEcole} — jamais d'un paramètre ni d'un corps de requête ;</li>
 *   <li>le module Vie scolaire est exigé en tête d'appel : sans abonnement le
 *       porteur d'un jeton de directeur reçoit un 403 ;</li>
 *   <li>rien ne sort en entité JPA : ces données personnelles de mineurs ne
 *       transitent que par des records DTO, et aucune ne va au journal.</li>
 * </ul>
 *
 * <p>Les dates sont au format ISO {@code aaaa-mm-jj}. Quand une période n'est
 * pas fournie, le mois en cours s'applique : voir {@link #periode}.
 */
@RestController
@RequestMapping("/api/ecole/absences")
public class AbsenceController {

    private final AbsenceService absenceService;
    private final EleveService eleveService;
    private final GroupeService groupeService;
    private final ModuleService moduleService;

    public AbsenceController(AbsenceService absenceService,
                             EleveService eleveService,
                             GroupeService groupeService,
                             ModuleService moduleService) {
        this.absenceService = absenceService;
        this.eleveService = eleveService;
        this.groupeService = groupeService;
        this.moduleService = moduleService;
    }

    /**
     * Liste filtrée des saisies d'une période. Tous les critères sont optionnels ;
     * un filtre de groupe ou de niveau écarte les élèves sans classe.
     *
     * @param justifiee {@code null} = justifiées et non justifiées confondues
     */
    @GetMapping
    public List<AbsenceReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant,
                                       @RequestParam(name = "debut", required = false) String debut,
                                       @RequestParam(name = "fin", required = false) String fin,
                                       @RequestParam(name = "groupeId", required = false) Long groupeId,
                                       @RequestParam(name = "niveauId", required = false) Long niveauId,
                                       @RequestParam(name = "eleveId", required = false) Long eleveId,
                                       @RequestParam(name = "type", required = false) String type,
                                       @RequestParam(name = "justifiee", required = false) Boolean justifiee) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);

        Periode periode = periode(debut, fin);
        List<Absence> absences = absenceService.rechercher(etablissementId, periode.debut(), periode.fin(),
                groupeId, niveauId, eleveId, enumeration(TypeAbsence.class, type, "type"), justifiee);
        return absences.stream().map(MappeursAbsence::versAbsenceReponse).toList();
    }

    /**
     * Feuille d'appel d'un groupe : tous ses élèves, chacun avec la saisie déjà
     * enregistrée pour ce contexte ou {@code null} s'il est présent.
     *
     * @param date        jour appelé, aujourd'hui par défaut
     * @param demiJournee MATIN par défaut
     * @param seanceId    séance appelée, absent pour un appel de demi-journée
     */
    @GetMapping("/feuille")
    public FeuilleAppelReponse feuille(@AuthenticationPrincipal UtilisateurCourant courant,
                                       @RequestParam(name = "groupeId") Long groupeId,
                                       @RequestParam(name = "date", required = false) String date,
                                       @RequestParam(name = "demiJournee", required = false) String demiJournee,
                                       @RequestParam(name = "seanceId", required = false) Long seanceId) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);

        LocalDate jour = jourOuAujourdhui(date);
        DemiJournee contexte = contexte(demiJournee);
        // Chargements filtrés par locataire : groupe et élèves d'un autre établissement restent hors d'atteinte.
        Groupe groupe = groupeService.obtenir(etablissementId, groupeId);
        List<Eleve> eleves = eleveService.listerParGroupe(etablissementId, groupeId);

        Map<Long, Absence> saisiesParEleve = new HashMap<>();
        for (Absence absence : absenceService.listerFeuille(etablissementId, groupeId, jour, contexte, seanceId)) {
            saisiesParEleve.put(absence.getEleve().getId(), absence);
        }
        return MappeursAbsence.versFeuilleAppelReponse(groupe, jour, contexte, seanceId, eleves, saisiesParEleve);
    }

    /**
     * Saisie en lot d'une feuille d'appel, transactionnelle et idempotente.
     *
     * <p>La feuille fait foi : une ligne de {@code type} nul déclare l'élève
     * présent et n'entre donc pas dans la feuille, ce qui supprime sa saisie
     * antérieure. Un élève du groupe absent de la liste est traité de même.
     */
    @PostMapping("/feuille")
    public ResultatFeuilleReponse enregistrerFeuille(@AuthenticationPrincipal UtilisateurCourant courant,
                                                     @Valid @RequestBody SaisieFeuilleRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);

        List<LigneAppelRequete> soumises = requete.saisies() == null ? List.of() : requete.saisies();
        List<LigneAppel> lignes = new ArrayList<>(soumises.size());
        for (LigneAppelRequete saisie : soumises) {
            if (saisie.present()) {
                continue;
            }
            lignes.add(new LigneAppel(saisie.eleveId(), saisie.type(), saisie.justifieeOuFaux(), saisie.motif()));
        }

        ResultatFeuille resultat = absenceService.appliquerFeuille(etablissementId, requete.groupeId(),
                requete.date(), requete.demiJournee(), requete.seanceId(), lignes,
                courant == null ? null : courant.id());
        return new ResultatFeuilleReponse(resultat.crees(), resultat.misAJour(), resultat.supprimes());
    }

    /**
     * Correction d'une saisie : justification, motif ou nature. Champ absent ou
     * nul = valeur inchangée ; motif vide = motif effacé.
     */
    @PatchMapping("/{id}")
    public AbsenceReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                   @PathVariable Long id,
                                   @Valid @RequestBody ModificationAbsenceRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);
        Absence absence = absenceService.modifier(etablissementId, id, requete.justifiee(), requete.type(),
                requete.motif());
        return MappeursAbsence.versAbsenceReponse(absence);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);
        absenceService.supprimer(etablissementId, id);
    }

    /**
     * Statistiques d'absentéisme : totaux, série temporelle et répartition par
     * classe.
     *
     * <p>Le taux publié rapporte les demi-journées d'absence (type ABSENCE seul,
     * une absence JOURNEE en valant deux) au volume dû, soit
     * <em>effectif INSCRIT × jours ouvrables × 2</em>, les jours ouvrables étant
     * les jours de la période hors dimanche. Retards et exclusions sont
     * dénombrés à part et n'entrent jamais dans le taux. Résultat en pourcentage
     * arrondi au dixième. La formule complète est documentée sur
     * {@code AbsenceService.synthese}, qui la calcule.
     *
     * @param periodeDemandee granularité des séries, JOUR par défaut
     */
    @GetMapping("/statistiques")
    public StatistiquesAbsencesReponse statistiques(
            @AuthenticationPrincipal UtilisateurCourant courant,
            @RequestParam(name = "periode", required = false) String periodeDemandee,
            @RequestParam(name = "debut", required = false) String debut,
            @RequestParam(name = "fin", required = false) String fin,
            @RequestParam(name = "groupeId", required = false) Long groupeId,
            @RequestParam(name = "niveauId", required = false) Long niveauId) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);

        Periode periode = periode(debut, fin);
        return MappeursAbsence.versStatistiquesReponse(absenceService.synthese(etablissementId,
                PeriodeStatistiques.depuisCode(periodeDemandee), periode.debut(), periode.fin(),
                groupeId, niveauId));
    }

    /**
     * Élèves dont les absences non justifiées atteignent le seuil sur la période.
     *
     * @param seuil absent, nul ou négatif = {@link AbsenceService#SEUIL_ALERTE_PAR_DEFAUT}
     */
    @GetMapping("/alertes")
    public List<AlerteAbsenceReponse> alertes(@AuthenticationPrincipal UtilisateurCourant courant,
                                              @RequestParam(name = "seuil", required = false) Long seuil,
                                              @RequestParam(name = "debut", required = false) String debut,
                                              @RequestParam(name = "fin", required = false) String fin) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);

        Periode periode = periode(debut, fin);
        return absenceService.alertes(etablissementId, periode.debut(), periode.fin(),
                        seuil == null ? 0L : seuil).stream()
                .map(MappeursAbsence::versAlerteReponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Lecture des paramètres de requête
    // ------------------------------------------------------------------

    /** Bornes de la période effectivement interrogée. */
    private record Periode(LocalDate debut, LocalDate fin) {
    }

    /**
     * Période interrogée, complétée quand la requête ne la fournit pas :
     * <ul>
     *   <li>aucune date : du premier jour du mois en cours à aujourd'hui ;</li>
     *   <li>fin seule : depuis le premier jour de son mois ;</li>
     *   <li>début seul : jusqu'au dernier jour de son mois.</li>
     * </ul>
     * Une fin antérieure au début est refusée par le service (400).
     */
    private static Periode periode(String debut, String fin) {
        LocalDate depuis = jour(debut, "debut");
        LocalDate jusqua = jour(fin, "fin");
        if (depuis == null && jusqua == null) {
            jusqua = LocalDate.now();
            depuis = jusqua.withDayOfMonth(1);
        } else if (depuis == null) {
            depuis = jusqua.withDayOfMonth(1);
        } else if (jusqua == null) {
            jusqua = depuis.withDayOfMonth(1).plusMonths(1).minusDays(1);
        }
        return new Periode(depuis, jusqua);
    }

    private static LocalDate jourOuAujourdhui(String valeur) {
        LocalDate jour = jour(valeur, "date");
        return jour == null ? LocalDate.now() : jour;
    }

    private static DemiJournee contexte(String valeur) {
        DemiJournee demiJournee = enumeration(DemiJournee.class, valeur, "demiJournee");
        return demiJournee == null ? DemiJournee.MATIN : demiJournee;
    }

    /**
     * Date ISO d'un paramètre de requête. Valeur absente = {@code null} (pas de
     * critère) ; valeur illisible = 400 plutôt que 500.
     */
    private static LocalDate jour(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valeur.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Date invalide pour « " + champ + " » : format attendu aaaa-mm-jj");
        }
    }

    /**
     * Valeur d'énumération d'un paramètre de requête, insensible à la casse.
     * Valeur absente = {@code null} (pas de critère) ; valeur inconnue = 400,
     * message énumérant les valeurs acceptées.
     */
    private static <T extends Enum<T>> T enumeration(Class<T> type, String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            String acceptees = String.join(", ",
                    Stream.of(type.getEnumConstants()).map(Enum::name).toList());
            throw new IllegalArgumentException("Valeur invalide pour « " + champ
                    + " » : valeurs acceptées " + acceptees);
        }
    }
}
