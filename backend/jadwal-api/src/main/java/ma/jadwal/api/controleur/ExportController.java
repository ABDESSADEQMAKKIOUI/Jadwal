package ma.jadwal.api.controleur;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.jadwal.api.export.ExportPlanningService;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.planning.entite.SemaineSeance;

/**
 * Exports de documents (L6). Pour l'instant : PDF des emplois du temps.
 */
@RestController
@RequestMapping("/api/ecole/exports")
public class ExportController {

    private final ExportPlanningService exportPlanningService;

    public ExportController(ExportPlanningService exportPlanningService) {
        this.exportPlanningService = exportPlanningService;
    }

    /**
     * PDF des emplois du temps, une page par groupe.
     *
     * @param groupeId  optionnel : un seul groupe ; absent = tous les groupes
     * @param versionId optionnel : version de planning ; absent = version active
     * @param semaine   optionnel : TOUTES (défaut), A ou B
     */
    @GetMapping(value = "/plannings.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exporterPlannings(@AuthenticationPrincipal UtilisateurCourant courant,
                                                    @RequestParam(required = false) Long groupeId,
                                                    @RequestParam(required = false) Long versionId,
                                                    @RequestParam(required = false) String semaine) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        SemaineSeance filtre = semaine == null || semaine.isBlank()
                ? SemaineSeance.TOUTES
                : SemaineSeance.valueOf(semaine.toUpperCase());

        byte[] pdf = exportPlanningService.exporter(etablissementId, groupeId, versionId, filtre);
        String nom = exportPlanningService.nomFichier(groupeId, etablissementId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .body(pdf);
    }
}
