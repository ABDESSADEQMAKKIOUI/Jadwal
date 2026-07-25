package ma.jadwal.solver.validation;

import java.util.ArrayList;
import java.util.List;

import ma.jadwal.solver.contrainte.CalculsPlanning;
import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.SeancePlan;

/**
 * Validation synchrone d'un déplacement manuel contre les règles dures (I-04).
 * <p>
 * Vérifie B-01, B-02 (avec hiérarchie parent/sous-groupe), B-03, D-01, D-02, D-03,
 * E-01, E-02, E-03, F-01 et G-01 pour une séance candidate placée parmi les séances existantes.
 * La séance existante portant le même id que la candidate est ignorée (c'est elle qu'on déplace).
 */
public class ValidationConflits {

    public List<ConflitDur> verifier(SeancePlan candidate, List<SeancePlan> existantes) {
        List<ConflitDur> conflits = new ArrayList<>();
        if (candidate.getCreneau() == null) {
            return conflits;
        }
        List<SeancePlan> autres = new ArrayList<>();
        for (SeancePlan existante : existantes) {
            if (existante.getCreneau() == null
                    || (candidate.getId() != null && candidate.getId().equals(existante.getId()))) {
                continue;
            }
            autres.add(existante);
        }

        String position = candidate.getCreneau().jour() + " à l'index " + candidate.getCreneau().indexDebut();

        // B-01 / B-02 / B-03 — chevauchements
        for (SeancePlan autre : autres) {
            if (!CalculsPlanning.seChevauchent(candidate, autre)) {
                continue;
            }
            if (candidate.getEnseignant() != null && autre.getEnseignant() != null
                    && candidate.getEnseignant().getId() == autre.getEnseignant().getId()) {
                conflits.add(new ConflitDur("B-01", "L'enseignant " + candidate.getEnseignant().getNom()
                        + " a déjà une séance de " + autre.getMatiere().libelle() + " avec "
                        + autre.getGroupe().libelle() + " le " + position + "."));
            }
            if (candidate.getGroupe() != null && candidate.getGroupe().estLieA(autre.getGroupe())) {
                conflits.add(new ConflitDur("B-02", "Le groupe " + autre.getGroupe().libelle()
                        + " a déjà une séance de " + autre.getMatiere().libelle() + " le " + position + "."));
            }
            if (candidate.getSalle() != null && autre.getSalle() != null
                    && candidate.getSalle().id() == autre.getSalle().id()) {
                conflits.add(new ConflitDur("B-03", "La salle " + candidate.getSalle().nom()
                        + " est déjà occupée par " + autre.getGroupe().libelle() + " le " + position + "."));
            }
        }

        // D-01 — quota hebdomadaire de l'enseignant
        if (candidate.getEnseignant() != null) {
            List<SeancePlan> chargees = new ArrayList<>();
            chargees.add(candidate);
            for (SeancePlan autre : autres) {
                if (autre.getEnseignant() != null
                        && autre.getEnseignant().getId() == candidate.getEnseignant().getId()) {
                    chargees.add(autre);
                }
            }
            long charge = CalculsPlanning.charge(chargees);
            int quota = candidate.getEnseignant().getQuotaHebdoUnites();
            if (charge > quota) {
                conflits.add(new ConflitDur("D-01", "Ce placement porte la charge de "
                        + candidate.getEnseignant().getNom() + " à " + charge + " unités pour un quota de "
                        + quota + " unités : dépassement de " + (charge - quota) + " unités."));
            }

            // D-02 — habilitation
            if (!candidate.getEnseignant().estHabilite(candidate.getMatiere().id(),
                    candidate.getGroupe().niveauId())) {
                conflits.add(new ConflitDur("D-02", "L'enseignant " + candidate.getEnseignant().getNom()
                        + " n'est pas habilité pour " + candidate.getMatiere().libelle()
                        + " au niveau du groupe " + candidate.getGroupe().libelle() + "."));
            }

            // D-03 — indisponibilités validées
            for (IndispoPlan indispo : candidate.getEnseignant().getIndisponibilites()) {
                if (CalculsPlanning.conflitIndispo(candidate, indispo)) {
                    conflits.add(new ConflitDur("D-03", "L'enseignant " + candidate.getEnseignant().getNom()
                            + " est indisponible le " + indispo.jour() + " de l'index " + indispo.indexDebut()
                            + " à " + indispo.finExclu() + "."));
                    break;
                }
            }
        }

        // E-01 / E-02 / E-03 — salle
        if (candidate.getSalle() != null) {
            if (candidate.getSalle().capacite() < candidate.getGroupe().effectif()) {
                conflits.add(new ConflitDur("E-01", "La salle " + candidate.getSalle().nom() + " ("
                        + candidate.getSalle().capacite() + " places) est trop petite pour "
                        + candidate.getGroupe().libelle() + " (" + candidate.getGroupe().effectif()
                        + " élèves)."));
            }
            if (candidate.getMatiere().typeSalleRequis() != null
                    && !candidate.getMatiere().typeSalleRequis().equals(candidate.getSalle().type())) {
                conflits.add(new ConflitDur("E-02", candidate.getMatiere().libelle() + " exige une salle de type "
                        + candidate.getMatiere().typeSalleRequis() + ", or " + candidate.getSalle().nom()
                        + " est de type " + candidate.getSalle().type() + "."));
            }
            if (!candidate.getSalle().equipements().containsAll(candidate.getMatiere().equipementsRequis())) {
                conflits.add(new ConflitDur("E-03", "La salle " + candidate.getSalle().nom()
                        + " ne possède pas tous les équipements requis par "
                        + candidate.getMatiere().libelle() + " : " + candidate.getMatiere().equipementsRequis()
                        + " requis, " + candidate.getSalle().equipements() + " présents."));
            }
        }

        // F-01 — plafond journalier du couple (groupe, matière)
        if (candidate.getMaxParJourUnites() > 0) {
            long chargeJour = candidate.getDureeUnites();
            for (SeancePlan autre : autres) {
                if (autre.getGroupe().id() == candidate.getGroupe().id()
                        && autre.getMatiere().id() == candidate.getMatiere().id()
                        && autre.getCreneau().jour() == candidate.getCreneau().jour()
                        && autre.getSemaine().chevauche(candidate.getSemaine())) {
                    chargeJour += autre.getDureeUnites();
                }
            }
            if (chargeJour > candidate.getMaxParJourUnites()) {
                conflits.add(new ConflitDur("F-01", "Le groupe " + candidate.getGroupe().libelle()
                        + " atteindrait " + chargeJour + " unités de " + candidate.getMatiere().libelle()
                        + " ce jour, pour un plafond de " + candidate.getMaxParJourUnites() + " unités."));
            }
        }

        // G-01 — débordement de la plage contiguë
        if (candidate.getDureeUnites() > candidate.getCreneau().unitesDisponibles()) {
            conflits.add(new ConflitDur("G-01", "La séance de " + candidate.getDureeUnites()
                    + " unités déborde de la plage : seulement " + candidate.getCreneau().unitesDisponibles()
                    + " unités contiguës disponibles à partir de ce créneau."));
        }

        return conflits;
    }
}
