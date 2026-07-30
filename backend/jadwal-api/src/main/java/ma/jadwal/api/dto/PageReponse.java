package ma.jadwal.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe de pagination du contrat REST :
 * {@code {"contenu":[...],"total":n,"page":0,"taille":50}}.
 */
public record PageReponse<T>(List<T> contenu, long total, int page, int taille) {

    /**
     * Convertit une page d'entités en page de DTOs : aucune entité JPA ne doit
     * être sérialisée telle quelle.
     */
    public static <E, T> PageReponse<T> depuis(Page<E> page, Function<E, T> mappeur) {
        return new PageReponse<>(page.getContent().stream().map(mappeur).toList(),
                page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
