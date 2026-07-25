package ma.jadwal.export.modele;

import java.util.List;

/**
 * Grille hebdomadaire d'un groupe (une page du PDF).
 *
 * @param intitule libellé de gauche, ex. « Classe : 1AC / A »
 * @param jours    en-têtes de colonnes, ex. [Lundi, Mardi, …]
 * @param lignes   lignes de la grille, dans l'ordre chronologique
 */
public record GrillePlanning(String intitule, List<String> jours, List<LignePlanning> lignes) {
}
