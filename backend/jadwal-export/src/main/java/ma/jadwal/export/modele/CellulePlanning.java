package ma.jadwal.export.modele;

/**
 * Contenu d'une case de la grille.
 *
 * @param matiere      libellé de la matière ; {@code null} = case vide (aucun cours)
 * @param details      seconde ligne optionnelle (enseignant, salle) ; peut être {@code null}
 * @param nbLignes     hauteur de la case en nombre de tranches horaires (fusion verticale)
 * @param couleurHexa  couleur de la matière (« #6366f1 ») utilisée en fond très clair ; peut être {@code null}
 */
public record CellulePlanning(String matiere, String details, int nbLignes, String couleurHexa) {

    public static CellulePlanning vide() {
        return new CellulePlanning(null, null, 1, null);
    }

    public boolean estVide() {
        return matiere == null;
    }
}
