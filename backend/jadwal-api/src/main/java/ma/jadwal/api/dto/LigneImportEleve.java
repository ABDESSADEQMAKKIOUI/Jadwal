package ma.jadwal.api.dto;

import java.util.List;

/**
 * Une ligne du fichier Massar telle que l'analyse l'a comprise.
 *
 * @param numero   rang de la ligne dans le fichier (l'en-tête est la ligne 1) ;
 *                 objet et non primitif, car Jackson 3 refuse un corps de
 *                 requête où un champ primitif est absent
 * @param donnees  champs reconnus, éventuellement incomplets
 * @param statut   NOUVEAU, EXISTANT ou ERREUR
 * @param messages explications destinées à l'humain qui valide l'import
 */
public record LigneImportEleve(
        Integer numero,
        EleveReponse donnees,
        String statut,
        List<String> messages) {

    /** Ligne à écrire : un nouvel élève ou un élève déjà inscrit. */
    public static final String NOUVEAU = "NOUVEAU";
    public static final String EXISTANT = "EXISTANT";

    /** Ligne inexploitable : elle ne sera jamais écrite, même si le client la renvoie. */
    public static final String ERREUR = "ERREUR";
}
