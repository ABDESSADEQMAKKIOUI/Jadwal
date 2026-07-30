package ma.jadwal.api.erreur;

import ma.jadwal.api.securite.IdentifiantsInvalidesException;
import ma.jadwal.common.ApiError;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.ModuleNonSouscritException;
import ma.jadwal.common.exception.OperationNonAutoriseeException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GestionnaireExceptions {

    private static final Logger journal = LoggerFactory.getLogger(GestionnaireExceptions.class);

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(RessourceIntrouvableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, exception.getMessage()));
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiError> gererConflit(ConflitException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, exception.getMessage()));
    }

    @ExceptionHandler(OperationNonAutoriseeException.class)
    public ResponseEntity<ApiError> gererNonAutorisee(OperationNonAutoriseeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, exception.getMessage()));
    }

    @ExceptionHandler(ModuleNonSouscritException.class)
    public ResponseEntity<ApiError> gererModuleNonSouscrit(ModuleNonSouscritException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, exception.getMessage(), Map.of("module", exception.getModule())));
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ApiError> gererIdentifiantsInvalides(IdentifiantsInvalidesException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, exception.getMessage()));
    }

    @ExceptionHandler(FaisabiliteEchecException.class)
    public ResponseEntity<Map<String, Object>> gererFaisabiliteEchec(FaisabiliteEchecException exception) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("statut", 422);
        corps.put("message", exception.getMessage());
        corps.put("rapport", exception.getRapport());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(corps);
    }

    @ExceptionHandler(ConflitsSeanceException.class)
    public ResponseEntity<Map<String, Object>> gererConflitsSeance(ConflitsSeanceException exception) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("statut", 409);
        corps.put("message", exception.getMessage());
        corps.put("conflits", exception.getConflits());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corps);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> gererIntegrite(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "Opération impossible : des données liées y font encore référence."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erreur -> details.put(erreur.getField(), erreur.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Requête invalide", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> gererCorpsIllisible(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Corps de requête invalide"));
    }

    /**
     * Fichier téléversé plus gros que la limite du conteneur : la requête est
     * coupée avant d'atteindre le contrôleur, on rend un message explicite
     * plutôt qu'une erreur interne.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> gererFichierTropVolumineux(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError(413, "Le fichier dépasse la taille maximale autorisée (5 Mo)."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> gererPartieManquante(MissingServletRequestPartException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Fichier manquant : envoyez le champ « fichier » en multipart/form-data."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> gererArgumentInvalide(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> gererRouteInconnue(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, "Ressource introuvable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> gererErreurInterne(Exception exception) {
        journal.error("Erreur interne non gérée", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Erreur interne du serveur"));
    }
}
