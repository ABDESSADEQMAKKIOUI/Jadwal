package ma.jadwal.api.dto;

import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.scolarite.entite.Absence;
import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.service.AbsencesParGroupe;
import ma.jadwal.scolarite.service.AlerteAbsenteisme;
import ma.jadwal.scolarite.service.SerieAbsences;
import ma.jadwal.scolarite.service.StatistiquesAbsences;
import ma.jadwal.scolarite.service.SyntheseAbsences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Conversion des entités et agrégats de vie scolaire vers les DTO de réponse.
 *
 * <p>Aucune entité JPA n'est sérialisée directement : {@code Eleve.groupe} est
 * EAGER et entraînerait tout le référentiel dans la réponse, et les champs
 * tuteur ne doivent pas fuir hors des écrans qui en ont besoin. Ces mappeurs
 * sont donc le seul chemin de sortie des données de vie scolaire.
 */
public final class MappeursAbsence {

    private MappeursAbsence() {
    }

    public static AbsenceReponse versAbsenceReponse(Absence absence) {
        Eleve eleve = absence.getEleve();
        Groupe groupe = eleve == null ? null : eleve.getGroupe();
        return new AbsenceReponse(
                absence.getId(),
                eleve == null ? null : eleve.getId(),
                nomComplet(eleve),
                eleve == null ? null : eleve.getCodeMassar(),
                groupe == null ? null : groupe.getLibelle(),
                absence.getDateAbsence(),
                nomEnum(absence.getDemiJournee()),
                nomEnum(absence.getType()),
                absence.isJustifiee(),
                absence.getMotif());
    }

    public static SaisieAbsenceReponse versSaisieReponse(Absence absence) {
        return new SaisieAbsenceReponse(
                absence.getId(),
                nomEnum(absence.getType()),
                nomEnum(absence.getDemiJournee()),
                absence.isJustifiee(),
                absence.getMotif());
    }

    /**
     * Feuille d'appel complète : chaque élève du groupe, avec sa saisie
     * éventuelle. L'absence d'entrée dans {@code saisiesParEleve} vaut présence.
     */
    public static FeuilleAppelReponse versFeuilleAppelReponse(Groupe groupe, LocalDate date,
                                                              DemiJournee demiJournee, Long seanceId,
                                                              List<Eleve> eleves,
                                                              Map<Long, Absence> saisiesParEleve) {
        List<EleveFeuilleReponse> lignes = new ArrayList<>(eleves.size());
        for (Eleve eleve : eleves) {
            Absence saisie = saisiesParEleve.get(eleve.getId());
            lignes.add(new EleveFeuilleReponse(
                    eleve.getId(),
                    eleve.getNom(),
                    eleve.getPrenom(),
                    eleve.getCodeMassar(),
                    nomEnum(eleve.getStatut()),
                    saisie == null ? null : versSaisieReponse(saisie)));
        }
        return new FeuilleAppelReponse(versGroupeFeuilleReponse(groupe), date, nomEnum(demiJournee), seanceId,
                List.copyOf(lignes));
    }

    public static GroupeFeuilleReponse versGroupeFeuilleReponse(Groupe groupe) {
        Niveau niveau = groupe.getNiveau();
        return new GroupeFeuilleReponse(
                groupe.getId(),
                groupe.getLibelle(),
                niveau == null ? null : niveau.getId(),
                niveau == null ? null : niveau.getLibelle(),
                groupe.getEffectif());
    }

    public static StatistiquesAbsencesReponse versStatistiquesReponse(SyntheseAbsences synthese) {
        StatistiquesAbsences totaux = synthese.totaux();
        List<SerieAbsencesReponse> series = new ArrayList<>(synthese.series().size());
        for (SerieAbsences point : synthese.series()) {
            series.add(new SerieAbsencesReponse(point.cle(), point.libelle(), point.absences(), point.retards()));
        }
        List<GroupeAbsencesReponse> parGroupe = new ArrayList<>(synthese.parGroupe().size());
        for (AbsencesParGroupe ligne : synthese.parGroupe()) {
            parGroupe.add(new GroupeAbsencesReponse(ligne.groupeId(), ligne.groupeLibelle(), ligne.effectif(),
                    ligne.absences(), ligne.tauxAbsenteisme()));
        }
        return new StatistiquesAbsencesReponse(
                nomEnum(synthese.periode()),
                synthese.debut(),
                synthese.fin(),
                new TotauxAbsencesReponse(
                        totaux.totalAbsences(),
                        totaux.retards(),
                        totaux.exclusions(),
                        totaux.absencesJustifiees(),
                        totaux.absencesNonJustifiees(),
                        totaux.tauxAbsenteisme(),
                        totaux.elevesInscrits(),
                        totaux.joursOuvrables()),
                List.copyOf(series),
                List.copyOf(parGroupe));
    }

    public static AlerteAbsenceReponse versAlerteReponse(AlerteAbsenteisme alerte) {
        return new AlerteAbsenceReponse(
                alerte.eleveId(),
                nomComplet(alerte.nom(), alerte.prenom()),
                alerte.codeMassar(),
                alerte.groupeId(),
                alerte.groupeLibelle(),
                alerte.absencesNonJustifiees());
    }

    private static String nomComplet(Eleve eleve) {
        return eleve == null ? null : nomComplet(eleve.getNom(), eleve.getPrenom());
    }

    private static String nomComplet(String nom, String prenom) {
        String assemble = (nom == null ? "" : nom) + " " + (prenom == null ? "" : prenom);
        String propre = assemble.trim();
        return propre.isEmpty() ? null : propre;
    }

    private static String nomEnum(Enum<?> valeur) {
        return valeur == null ? null : valeur.name();
    }
}
