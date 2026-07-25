package ma.jadwal.enseignant.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.enseignant.depot.EnseignantRepository;
import ma.jadwal.enseignant.depot.HabilitationRepository;
import ma.jadwal.enseignant.depot.IndisponibiliteRepository;
import ma.jadwal.enseignant.depot.PreferenceHoraireRepository;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.enseignant.entite.Habilitation;
import ma.jadwal.enseignant.entite.Indisponibilite;
import ma.jadwal.enseignant.entite.PreferenceHoraire;
import ma.jadwal.enseignant.entite.Semaine;
import ma.jadwal.enseignant.entite.SourceIndispo;
import ma.jadwal.enseignant.entite.StatutIndispo;
import ma.jadwal.enseignant.entite.TypeEnseignant;
import ma.jadwal.enseignant.entite.TypePreference;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.depot.NiveauRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EnseignantService {

    public record HabilitationDemande(Long matiereId, List<Long> niveauIds) {
    }

    public record PreferenceDemande(Jour jour, int indexDebut, int dureeUnites, TypePreference type) {
    }

    private final EnseignantRepository enseignantRepository;
    private final HabilitationRepository habilitationRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final PreferenceHoraireRepository preferenceHoraireRepository;
    private final EtablissementRepository etablissementRepository;
    private final MatiereRepository matiereRepository;
    private final NiveauRepository niveauRepository;

    public EnseignantService(EnseignantRepository enseignantRepository,
                             HabilitationRepository habilitationRepository,
                             IndisponibiliteRepository indisponibiliteRepository,
                             PreferenceHoraireRepository preferenceHoraireRepository,
                             EtablissementRepository etablissementRepository,
                             MatiereRepository matiereRepository,
                             NiveauRepository niveauRepository) {
        this.enseignantRepository = enseignantRepository;
        this.habilitationRepository = habilitationRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.preferenceHoraireRepository = preferenceHoraireRepository;
        this.etablissementRepository = etablissementRepository;
        this.matiereRepository = matiereRepository;
        this.niveauRepository = niveauRepository;
    }

    // ---------- Enseignants ----------

    @Transactional(readOnly = true)
    public List<Enseignant> lister(Long etablissementId) {
        return enseignantRepository.findByEtablissementIdOrderByNomCompletAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Enseignant obtenir(Long etablissementId, Long id) {
        return enseignantRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant introuvable : " + id));
    }

    @Transactional
    public Enseignant creer(Long etablissementId, String nomComplet, String email, TypeEnseignant type,
                            int quotaHebdoUnites, int maxConsecutifUnites, Integer amplitudeMaxUnites,
                            boolean vacataire, int bufferTrajetUnites) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        Enseignant enseignant = new Enseignant();
        enseignant.setEtablissement(etablissement);
        enseignant.setNomComplet(nomComplet);
        enseignant.setEmail(email);
        enseignant.setType(type != null ? type : TypeEnseignant.PROPRE);
        enseignant.setQuotaHebdoUnites(quotaHebdoUnites);
        enseignant.setMaxConsecutifUnites(maxConsecutifUnites);
        enseignant.setAmplitudeMaxUnites(amplitudeMaxUnites);
        enseignant.setVacataire(vacataire);
        enseignant.setBufferTrajetUnites(bufferTrajetUnites);
        return enseignantRepository.save(enseignant);
    }

    @Transactional
    public Enseignant mettreAJour(Long etablissementId, Long id, String nomComplet, String email,
                                  TypeEnseignant type, Integer quotaHebdoUnites, Integer maxConsecutifUnites,
                                  Integer amplitudeMaxUnites, Boolean vacataire, Integer bufferTrajetUnites) {
        Enseignant enseignant = obtenir(etablissementId, id);
        if (nomComplet != null) {
            enseignant.setNomComplet(nomComplet);
        }
        enseignant.setEmail(email);
        if (type != null) {
            enseignant.setType(type);
        }
        if (quotaHebdoUnites != null) {
            enseignant.setQuotaHebdoUnites(quotaHebdoUnites);
        }
        if (maxConsecutifUnites != null) {
            enseignant.setMaxConsecutifUnites(maxConsecutifUnites);
        }
        enseignant.setAmplitudeMaxUnites(amplitudeMaxUnites);
        if (vacataire != null) {
            enseignant.setVacataire(vacataire);
        }
        if (bufferTrajetUnites != null) {
            enseignant.setBufferTrajetUnites(bufferTrajetUnites);
        }
        return enseignantRepository.save(enseignant);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Enseignant enseignant = obtenir(etablissementId, id);
        enseignantRepository.delete(enseignant);
    }

    // ---------- Habilitations ----------

    @Transactional(readOnly = true)
    public List<Habilitation> listerHabilitations(Long etablissementId, Long enseignantId) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        return habilitationRepository.findByEnseignantId(enseignant.getId());
    }

    @Transactional
    public List<Habilitation> remplacerHabilitations(Long etablissementId, Long enseignantId,
                                                     List<HabilitationDemande> demandes) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        habilitationRepository.deleteByEnseignantId(enseignant.getId());
        habilitationRepository.flush();

        List<Habilitation> resultat = new ArrayList<>();
        if (demandes == null) {
            return resultat;
        }
        for (HabilitationDemande demande : demandes) {
            Matiere matiere = matiereRepository.findByIdAndEtablissementId(demande.matiereId(), etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Matière introuvable : " + demande.matiereId()));
            Habilitation habilitation = new Habilitation();
            habilitation.setEnseignant(enseignant);
            habilitation.setMatiere(matiere);
            Set<Niveau> niveaux = new LinkedHashSet<>();
            if (demande.niveauIds() != null) {
                for (Long niveauId : demande.niveauIds()) {
                    Niveau niveau = niveauRepository.findByIdAndEtablissementId(niveauId, etablissementId)
                            .orElseThrow(() -> new RessourceIntrouvableException("Niveau introuvable : " + niveauId));
                    niveaux.add(niveau);
                }
            }
            habilitation.setNiveaux(niveaux);
            resultat.add(habilitationRepository.save(habilitation));
        }
        return resultat;
    }

    // ---------- Indisponibilités ----------

    @Transactional(readOnly = true)
    public List<Indisponibilite> listerIndisponibilites(Long etablissementId, Long enseignantId) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        return indisponibiliteRepository.findByEnseignantIdOrderByJourAscIndexDebutAsc(enseignant.getId());
    }

    @Transactional
    public Indisponibilite creerIndisponibilite(Long etablissementId, Long enseignantId, SourceIndispo source,
                                                Jour jour, int indexDebut, int dureeUnites, Semaine semaine,
                                                String motif) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        Indisponibilite indisponibilite = new Indisponibilite();
        indisponibilite.setEnseignant(enseignant);
        indisponibilite.setSource(source != null ? source : SourceIndispo.PERSONNELLE);
        indisponibilite.setJour(jour);
        indisponibilite.setIndexDebut(indexDebut);
        indisponibilite.setDureeUnites(dureeUnites);
        indisponibilite.setSemaine(semaine != null ? semaine : Semaine.TOUTES);
        indisponibilite.setStatut(StatutIndispo.BROUILLON);
        indisponibilite.setMotif(motif);
        return indisponibiliteRepository.save(indisponibilite);
    }

    @Transactional
    public Indisponibilite mettreAJourIndisponibilite(Long etablissementId, Long enseignantId, Long indispoId,
                                                      SourceIndispo source, Jour jour, Integer indexDebut,
                                                      Integer dureeUnites, Semaine semaine, String motif) {
        Indisponibilite indisponibilite = obtenirIndisponibilite(etablissementId, enseignantId, indispoId);
        if (indisponibilite.getStatut() == StatutIndispo.VALIDE) {
            throw new ConflitException(
                    "Une indisponibilité validée ne peut plus être modifiée ; repassez-la d'abord en brouillon.");
        }
        if (source != null) {
            indisponibilite.setSource(source);
        }
        if (jour != null) {
            indisponibilite.setJour(jour);
        }
        if (indexDebut != null) {
            indisponibilite.setIndexDebut(indexDebut);
        }
        if (dureeUnites != null) {
            indisponibilite.setDureeUnites(dureeUnites);
        }
        if (semaine != null) {
            indisponibilite.setSemaine(semaine);
        }
        indisponibilite.setMotif(motif);
        return indisponibiliteRepository.save(indisponibilite);
    }

    @Transactional
    public Indisponibilite changerStatutIndisponibilite(Long etablissementId, Long enseignantId, Long indispoId,
                                                        StatutIndispo statut) {
        if (statut == null) {
            throw new ConflitException("Le statut cible est obligatoire.");
        }
        Indisponibilite indisponibilite = obtenirIndisponibilite(etablissementId, enseignantId, indispoId);
        if (indisponibilite.getStatut() == statut) {
            throw new ConflitException("L'indisponibilité est déjà au statut " + statut + ".");
        }
        indisponibilite.setStatut(statut);
        return indisponibiliteRepository.save(indisponibilite);
    }

    @Transactional
    public void supprimerIndisponibilite(Long etablissementId, Long enseignantId, Long indispoId) {
        Indisponibilite indisponibilite = obtenirIndisponibilite(etablissementId, enseignantId, indispoId);
        indisponibiliteRepository.delete(indisponibilite);
    }

    private Indisponibilite obtenirIndisponibilite(Long etablissementId, Long enseignantId, Long indispoId) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        return indisponibiliteRepository.findByIdAndEnseignantId(indispoId, enseignant.getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Indisponibilité introuvable : " + indispoId));
    }

    // ---------- Préférences horaires ----------

    @Transactional(readOnly = true)
    public List<PreferenceHoraire> listerPreferences(Long etablissementId, Long enseignantId) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        return preferenceHoraireRepository.findByEnseignantIdOrderByJourAscIndexDebutAsc(enseignant.getId());
    }

    @Transactional
    public List<PreferenceHoraire> remplacerPreferences(Long etablissementId, Long enseignantId,
                                                        List<PreferenceDemande> demandes) {
        Enseignant enseignant = obtenir(etablissementId, enseignantId);
        preferenceHoraireRepository.deleteByEnseignantId(enseignant.getId());
        preferenceHoraireRepository.flush();

        List<PreferenceHoraire> resultat = new ArrayList<>();
        if (demandes == null) {
            return resultat;
        }
        for (PreferenceDemande demande : demandes) {
            PreferenceHoraire preference = new PreferenceHoraire();
            preference.setEnseignant(enseignant);
            preference.setJour(demande.jour());
            preference.setIndexDebut(demande.indexDebut());
            preference.setDureeUnites(demande.dureeUnites());
            preference.setType(demande.type() != null ? demande.type() : TypePreference.EVITER);
            resultat.add(preferenceHoraireRepository.save(preference));
        }
        return resultat;
    }
}
