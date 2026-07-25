package ma.jadwal.api.dto;

import ma.jadwal.planning.entite.Seance;
import ma.jadwal.referentiel.entite.Creneau;

/**
 * Conversions entités planning -&gt; DTOs.
 */
public final class MappeursPlanning {

    private MappeursPlanning() {
    }

    public static SeanceReponse versSeanceReponse(Seance seance) {
        Creneau creneau = seance.getCreneau();
        return new SeanceReponse(
                seance.getId(),
                seance.getGroupe().getId(),
                seance.getGroupe().getLibelle(),
                seance.getMatiere().getId(),
                seance.getMatiere().getLibelle(),
                seance.getMatiere().getCouleur(),
                seance.getEnseignant() == null ? null : seance.getEnseignant().getId(),
                seance.getEnseignant() == null ? null : seance.getEnseignant().getNomComplet(),
                seance.getSalle() == null ? null : seance.getSalle().getId(),
                seance.getSalle() == null ? null : seance.getSalle().getNom(),
                creneau == null ? null : creneau.getId(),
                creneau == null ? null : creneau.getJour().name(),
                creneau == null ? null : creneau.getIndexDebut(),
                seance.getDureeUnites(),
                seance.getSemaine().name(),
                seance.isVerrouillee());
    }

    public static CreneauReponse versCreneauReponse(Creneau creneau) {
        return new CreneauReponse(
                creneau.getId(),
                creneau.getJour().name(),
                creneau.getIndexDebut(),
                creneau.getType().name(),
                creneau.getUnitesDisponibles());
    }
}
