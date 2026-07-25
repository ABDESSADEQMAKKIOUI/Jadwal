package ma.jadwal.api.dto;

import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.Paiement;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Utilisateur;

import java.math.BigDecimal;

/**
 * Conversion des entités vers les DTOs de réponse.
 */
public final class Mappeurs {

    private Mappeurs() {
    }

    public static UtilisateurReponse versUtilisateurReponse(Utilisateur utilisateur) {
        Etablissement etablissement = utilisateur.getEtablissement();
        return new UtilisateurReponse(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNomComplet(),
                utilisateur.getRole().name(),
                etablissement == null ? null : etablissement.getId(),
                etablissement == null ? null : etablissement.getNom());
    }

    public static CompteReponse versCompteReponse(Utilisateur utilisateur) {
        return new CompteReponse(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNomComplet(),
                utilisateur.getRole().name(),
                utilisateur.isActif());
    }

    public static EtablissementResumeReponse versEtablissementResumeReponse(Etablissement etablissement,
                                                                            Abonnement abonnementActif) {
        return new EtablissementResumeReponse(
                etablissement.getId(),
                etablissement.getNom(),
                etablissement.getCode(),
                etablissement.getVille(),
                etablissement.getTelephone(),
                etablissement.getEmail(),
                etablissement.getStatut().name(),
                etablissement.getDateCreation(),
                abonnementActif == null ? null : versAbonnementActifReponse(abonnementActif));
    }

    public static AbonnementActifReponse versAbonnementActifReponse(Abonnement abonnement) {
        return new AbonnementActifReponse(
                abonnement.getId(),
                abonnement.getPlan().getNom(),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getStatut().name());
    }

    public static AbonnementReponse versAbonnementReponse(Abonnement abonnement, BigDecimal totalPaye) {
        return new AbonnementReponse(
                abonnement.getId(),
                abonnement.getEtablissement().getId(),
                abonnement.getEtablissement().getNom(),
                abonnement.getPlan().getId(),
                abonnement.getPlan().getNom(),
                abonnement.getPlan().getPrixAnnuel(),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getStatut().name(),
                totalPaye);
    }

    public static AbonnementEtablissementReponse versAbonnementEtablissementReponse(Abonnement abonnement,
                                                                                    BigDecimal totalPaye) {
        return new AbonnementEtablissementReponse(
                abonnement.getId(),
                abonnement.getPlan().getId(),
                abonnement.getPlan().getNom(),
                abonnement.getPlan().getPrixAnnuel(),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getStatut().name(),
                totalPaye);
    }

    public static PlanReponse versPlanReponse(PlanAbonnement plan) {
        return new PlanReponse(
                plan.getId(),
                plan.getCode(),
                plan.getNom(),
                plan.getPrixAnnuel(),
                plan.getDescription(),
                plan.isActif());
    }

    public static PaiementReponse versPaiementReponse(Paiement paiement) {
        Abonnement abonnement = paiement.getAbonnement();
        return new PaiementReponse(
                paiement.getId(),
                abonnement.getId(),
                abonnement.getEtablissement().getId(),
                abonnement.getEtablissement().getNom(),
                abonnement.getPlan().getNom(),
                paiement.getMontant(),
                paiement.getMode().name(),
                paiement.getReference(),
                paiement.getDatePaiement(),
                paiement.getStatut().name(),
                paiement.getNote(),
                paiement.getDateCreation());
    }
}
