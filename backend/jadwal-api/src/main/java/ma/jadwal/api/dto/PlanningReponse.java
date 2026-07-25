package ma.jadwal.api.dto;

import java.util.List;

/**
 * Vue planning d'un groupe, d'un enseignant ou d'une salle : la grille et les séances.
 */
public record PlanningReponse(List<CreneauReponse> creneaux, List<SeanceReponse> seances) {
}
