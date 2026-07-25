package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ma.jadwal.api.dto.AnalyseReponse;
import ma.jadwal.api.dto.GenerationReponse;
import ma.jadwal.api.generation.GenerationService;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.planning.entite.Generation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Générations d'emploi du temps : lancement (202), suivi SSE, arrêt (I-05), analyse (I-06).
 */
@RestController
@RequestMapping("/api/ecole/generations")
public class GenerationController {

    public record GenerationRequete(@Min(10) @Max(600) Integer dureeMaxSecondes) {
    }

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public ResponseEntity<GenerationReponse> lancer(@AuthenticationPrincipal UtilisateurCourant courant,
                                                    @Valid @RequestBody(required = false)
                                                    GenerationRequete requete) {
        int dureeMaxSecondes = requete == null || requete.dureeMaxSecondes() == null
                ? 120 : requete.dureeMaxSecondes();
        Generation generation = generationService.lancer(ContexteEcole.etablissementId(courant),
                dureeMaxSecondes);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(GenerationReponse.depuis(generation));
    }

    @GetMapping
    public List<GenerationReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return generationService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(GenerationReponse::depuis)
                .toList();
    }

    @GetMapping("/{id}")
    public GenerationReponse obtenir(@AuthenticationPrincipal UtilisateurCourant courant,
                                     @PathVariable Long id) {
        return GenerationReponse.depuis(generationService.obtenir(ContexteEcole.etablissementId(courant), id));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suivre(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        return generationService.suivre(ContexteEcole.etablissementId(courant), id);
    }

    @PostMapping("/{id}/stop")
    public GenerationReponse arreter(@AuthenticationPrincipal UtilisateurCourant courant,
                                     @PathVariable Long id) {
        return GenerationReponse.depuis(generationService.arreter(ContexteEcole.etablissementId(courant), id));
    }

    @GetMapping("/{id}/analyse")
    public AnalyseReponse analyser(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        return generationService.analyser(ContexteEcole.etablissementId(courant), id);
    }
}
