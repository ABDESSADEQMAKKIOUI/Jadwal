package ma.jadwal.api.dto;

/**
 * Compteurs du rapport d'analyse d'un fichier Massar.
 */
public record ResumeImportEleves(int total, int nouveaux, int existants, int erreurs) {
}
