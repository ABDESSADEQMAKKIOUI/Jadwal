package ma.jadwal.api.dto;

import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.scolarite.entite.Eleve;

/**
 * Conversion des entités de scolarité vers les DTOs de réponse. Les entités
 * {@code Eleve} ne sont jamais sérialisées directement : leur groupe est EAGER
 * et remonterait tout le référentiel.
 */
public final class MappeursEleve {

    private MappeursEleve() {
    }

    public static EleveReponse versEleveReponse(Eleve eleve) {
        Groupe groupe = eleve.getGroupe();
        return new EleveReponse(
                eleve.getId(),
                eleve.getCodeMassar(),
                eleve.getNom(),
                eleve.getPrenom(),
                eleve.getNomAr(),
                eleve.getPrenomAr(),
                eleve.getDateNaissance(),
                eleve.getLieuNaissance(),
                eleve.getSexe() == null ? null : eleve.getSexe().name(),
                eleve.getStatut() == null ? null : eleve.getStatut().name(),
                groupe == null ? null : groupe.getId(),
                groupe == null ? null : groupe.getLibelle(),
                groupe == null || groupe.getNiveau() == null ? null : groupe.getNiveau().getLibelle(),
                eleve.getTuteurNom(),
                eleve.getTuteurTelephone());
    }
}
