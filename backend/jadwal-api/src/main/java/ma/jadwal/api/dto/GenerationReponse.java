package ma.jadwal.api.dto;

import ma.jadwal.planning.entite.Generation;

import java.time.Instant;

/**
 * État d'une génération d'emploi du temps.
 */
public record GenerationReponse(
        Long id,
        String statut,
        String score,
        int dureeMaxSecondes,
        Instant lanceeLe,
        Instant termineeLe,
        Long versionId) {

    public static GenerationReponse depuis(Generation generation) {
        return new GenerationReponse(
                generation.getId(),
                generation.getStatut().name(),
                generation.getScore(),
                generation.getDureeMaxSecondes(),
                generation.getLanceeLe(),
                generation.getTermineeLe(),
                generation.getVersion() == null ? null : generation.getVersion().getId());
    }
}
