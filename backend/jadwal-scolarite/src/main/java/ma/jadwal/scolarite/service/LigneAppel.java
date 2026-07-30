package ma.jadwal.scolarite.service;

import ma.jadwal.scolarite.entite.TypeAbsence;

/**
 * Une ligne de feuille d'appel : l'élève signalé et la nature du signalement.
 * La feuille ne contient QUE les élèves signalés ; les autres sont présents.
 */
public record LigneAppel(
        Long eleveId,
        TypeAbsence type,
        boolean justifiee,
        String motif) {

    public LigneAppel(Long eleveId) {
        this(eleveId, TypeAbsence.ABSENCE, false, null);
    }
}
