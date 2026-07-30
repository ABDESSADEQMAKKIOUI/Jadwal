package ma.jadwal.scolarite.service;

import ma.jadwal.scolarite.entite.Absence;

import java.util.List;

/**
 * Effet d'un enregistrement de feuille d'appel. La feuille faisant foi, une
 * saisie antérieure qui n'y figure plus est supprimée : les trois compteurs
 * rendent cette bascule lisible côté interface.
 *
 * <p>Rejouer la même feuille donne {@code crees = 0} et {@code supprimes = 0}.
 *
 * @param saisies    saisies persistées, dans l'ordre de la feuille
 * @param crees      saisies nouvellement créées
 * @param misAJour   saisies existantes réécrites
 * @param supprimes  saisies devenues absentes de la feuille, donc effacées
 */
public record ResultatFeuille(
        List<Absence> saisies,
        int crees,
        int misAJour,
        int supprimes) {
}
