package ma.jadwal.referentiel.service;

import java.util.List;

/**
 * Configuration de la grille horaire d'un établissement, sérialisée en JSON
 * dans la colonne etablissement.grille_json.
 */
public record GrilleConfig(
        List<String> joursActifs,
        String heureDebut,
        int dureeUniteMinutes,
        int unitesParJour,
        List<PlageBloquee> plagesBloquees) {

    public record PlageBloquee(String type, int indexDebut, int dureeUnites) {
    }
}
