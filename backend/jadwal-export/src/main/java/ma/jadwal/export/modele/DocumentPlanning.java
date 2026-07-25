package ma.jadwal.export.modele;

import java.util.List;

/**
 * Modèle d'entrée de l'export PDF des emplois du temps.
 * <p>
 * Volontairement indépendant de la persistance : le module d'export ne connaît ni JPA ni Spring.
 *
 * @param etablissementNom nom affiché en tête de page (ex. « Groupe scolaire Berrada »)
 * @param sousTitre        ligne sous le nom (ex. « Maternelle – Primaire – Collège - Lycée »)
 * @param anneeScolaire    ex. « 2026/2027 »
 * @param grilles          une grille par groupe : une page chacune
 */
public record DocumentPlanning(String etablissementNom, String sousTitre, String anneeScolaire,
                               List<GrillePlanning> grilles) {
}
