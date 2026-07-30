package ma.jadwal.scolarite.service;

import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;

import java.time.LocalDate;

/**
 * Données métier d'un élève, indépendamment du transport HTTP.
 * L'établissement n'y figure PAS : il vient toujours du JWT et est passé
 * séparément aux méthodes du service.
 */
public record DonneesEleve(
        String codeMassar,
        String nom,
        String prenom,
        String nomAr,
        String prenomAr,
        LocalDate dateNaissance,
        String lieuNaissance,
        Sexe sexe,
        StatutEleve statut,
        String tuteurNom,
        String tuteurTelephone,
        Long groupeId) {
}
