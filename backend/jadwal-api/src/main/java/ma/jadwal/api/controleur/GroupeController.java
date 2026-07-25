package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.TypeGroupe;
import ma.jadwal.referentiel.service.GroupeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groupes (classes) et sous-groupes de dédoublement (A-04).
 */
@RestController
@RequestMapping("/api/ecole/groupes")
public class GroupeController {

    public record GroupeReponse(Long id, String libelle, int effectif, Long niveauId, String niveauLibelle,
                                Long parentId, String type, List<GroupeReponse> sousGroupes) {
    }

    public record CreationGroupeRequete(@NotNull Long niveauId, @NotBlank String libelle,
                                        @PositiveOrZero int effectif) {
    }

    public record ModificationGroupeRequete(Long niveauId, String libelle, Integer effectif) {
    }

    public record SousGroupesRequete(@Min(2) @Max(4) int nombre) {
    }

    private final GroupeService groupeService;
    private final GroupeRepository groupeRepository;

    public GroupeController(GroupeService groupeService, GroupeRepository groupeRepository) {
        this.groupeService = groupeService;
        this.groupeRepository = groupeRepository;
    }

    @GetMapping
    public List<GroupeReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        List<Groupe> groupes = groupeService.lister(ContexteEcole.etablissementId(courant));
        Map<Long, List<Groupe>> enfants = new HashMap<>();
        for (Groupe groupe : groupes) {
            if (groupe.getParent() != null) {
                enfants.computeIfAbsent(groupe.getParent().getId(), id -> new ArrayList<>()).add(groupe);
            }
        }
        List<GroupeReponse> resultat = new ArrayList<>();
        for (Groupe groupe : groupes) {
            if (groupe.getParent() == null) {
                resultat.add(versReponse(groupe, enfants));
            }
        }
        return resultat;
    }

    @PostMapping
    public GroupeReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                               @Valid @RequestBody CreationGroupeRequete requete) {
        Groupe groupe = groupeService.creer(ContexteEcole.etablissementId(courant), requete.niveauId(),
                requete.libelle(), requete.effectif(), null, TypeGroupe.CLASSE);
        return versReponse(groupe, Map.of());
    }

    @PatchMapping("/{id}")
    public GroupeReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                  @PathVariable Long id,
                                  @RequestBody ModificationGroupeRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Groupe existant = groupeService.obtenir(etablissementId, id);
        Long parentId = existant.getParent() == null ? null : existant.getParent().getId();
        Groupe groupe = groupeService.mettreAJour(etablissementId, id, requete.niveauId(), requete.libelle(),
                requete.effectif(), parentId, null);
        Map<Long, List<Groupe>> enfants = Map.of(groupe.getId(), groupeRepository.findByParentId(groupe.getId()));
        return versReponse(groupe, enfants);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Groupe groupe = groupeService.obtenir(etablissementId, id);
        for (Groupe enfant : groupeRepository.findByParentId(groupe.getId())) {
            groupeService.supprimer(etablissementId, enfant.getId());
        }
        groupeService.supprimer(etablissementId, id);
    }

    /** Crée les sous-groupes « (G1) », « (G2) »… d'une classe, effectif réparti (A-04). */
    @PostMapping("/{id}/sous-groupes")
    @Transactional
    public GroupeReponse creerSousGroupes(@AuthenticationPrincipal UtilisateurCourant courant,
                                          @PathVariable Long id,
                                          @Valid @RequestBody SousGroupesRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Groupe parent = groupeService.obtenir(etablissementId, id);
        if (parent.getType() == TypeGroupe.SOUS_GROUPE) {
            throw new ConflitException("Un sous-groupe ne peut pas être dédoublé.");
        }
        if (!groupeRepository.findByParentId(parent.getId()).isEmpty()) {
            throw new ConflitException("Des sous-groupes existent déjà pour ce groupe.");
        }
        int nombre = requete.nombre();
        int base = parent.getEffectif() / nombre;
        int reste = parent.getEffectif() % nombre;
        for (int i = 1; i <= nombre; i++) {
            int effectif = base + (i <= reste ? 1 : 0);
            groupeService.creer(etablissementId, parent.getNiveau().getId(),
                    parent.getLibelle() + " (G" + i + ")", effectif, parent.getId(), TypeGroupe.SOUS_GROUPE);
        }
        Map<Long, List<Groupe>> enfants = Map.of(parent.getId(), groupeRepository.findByParentId(parent.getId()));
        return versReponse(parent, enfants);
    }

    private static GroupeReponse versReponse(Groupe groupe, Map<Long, List<Groupe>> enfantsParParent) {
        List<GroupeReponse> sousGroupes = enfantsParParent.getOrDefault(groupe.getId(), List.of()).stream()
                .map(enfant -> versReponse(enfant, Map.of()))
                .toList();
        return new GroupeReponse(groupe.getId(), groupe.getLibelle(), groupe.getEffectif(),
                groupe.getNiveau().getId(), groupe.getNiveau().getLibelle(),
                groupe.getParent() == null ? null : groupe.getParent().getId(),
                groupe.getType().name(), sousGroupes);
    }
}
