package ma.jadwal.api.dto;

import java.util.List;

/**
 * Rapport d'analyse d'un fichier Massar. Produit SANS aucune écriture en base :
 * c'est l'humain qui décide ensuite des lignes à retenir (même principe que la
 * règle D-04 sur la génération des emplois du temps).
 *
 * @param colonnesDetectees champs reconnus dans l'en-tête, dans l'ordre du fichier
 * @param colonnesIgnorees  en-têtes non reconnus, restitués tels quels
 * @param separateur        séparateur détecté (« ; » ou « , »)
 */
public record AnalyseImportReponse(
        List<String> colonnesDetectees,
        List<String> colonnesIgnorees,
        String separateur,
        List<LigneImportEleve> lignes,
        ResumeImportEleves resume) {
}
