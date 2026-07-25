package ma.jadwal.solver.generation;

import java.util.List;

import org.junit.jupiter.api.Test;

import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.sousGroupe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la factory de séances : C-01 (volume exact), C-02 (patterns), C-03 (durées min/max),
 * C-05 (quinzaine), A-04/B-04 (dédoublement et blocs alignés).
 */
class SeanceFactoryTest {

    @Test
    void c01_volumeHebdomadaireExactParGroupeEtMatiere() {
        var besoin = new BesoinSeances(groupe(1), matiere(1), 8, null, null, 4, "AUCUN",
                List.of(), null, null);

        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));

        assertThat(seances).isNotEmpty();
        assertThat(seances.stream().mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(8);
        assertThat(seances).allSatisfy(s -> {
            assertThat(s.getGroupe().id()).isEqualTo(1);
            assertThat(s.getMatiere().id()).isEqualTo(1);
            assertThat(s.getSemaine()).isEqualTo(SemainePlan.TOUTES);
            assertThat(s.getMaxParJourUnites()).isEqualTo(4);
        });
        // Ids déterministes "gId-mId-i".
        assertThat(seances.get(0).getId()).isEqualTo("1-1-0");
        assertThat(seances.get(1).getId()).isEqualTo("1-1-1");
        // Dénormalisations F-02 / F-03 : 2 séances sur 5 jours -> gapMin 2 ; charge 8/5 -> moyenne 2.
        assertThat(seances.get(0).getGapMinJours()).isEqualTo(2);
        assertThat(seances.get(0).getChargeMoyenneUnites()).isEqualTo(2);
    }

    @Test
    void c02_premierPatternDontLaSommeCorrespondAuVolume() {
        // Le premier pattern valide ([4,4]) est retenu.
        var besoin = new BesoinSeances(groupe(1), matiere(1), 8, null,
                List.of(List.of(4, 4), List.of(4, 2, 2)), 4, "AUCUN", List.of(), null, null);
        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));
        assertThat(seances).extracting(SeancePlan::getDureeUnites).containsExactly(4, 4);

        // Si le premier pattern ne correspond pas, le suivant est essayé.
        var besoin2 = new BesoinSeances(groupe(2), matiere(1), 8, null,
                List.of(List.of(4, 4, 4), List.of(4, 2, 2)), 4, "AUCUN", List.of(), null, null);
        List<SeancePlan> seances2 = SeanceFactory.genererSeances(List.of(besoin2));
        assertThat(seances2).extracting(SeancePlan::getDureeUnites).containsExactly(4, 2, 2);

        // Aucun pattern ne correspond : décomposition automatique en blocs de dureeMax (4).
        var besoin3 = new BesoinSeances(groupe(3), matiere(1), 8, null,
                List.of(List.of(3, 3)), 4, "AUCUN", List.of(), null, null);
        List<SeancePlan> seances3 = SeanceFactory.genererSeances(List.of(besoin3));
        assertThat(seances3).extracting(SeancePlan::getDureeUnites).containsExactly(4, 4);
    }

    @Test
    void c03_dureesGenereesRespectentMinEtMaxDeLaMatiere() {
        // Volume 9, durées bornées [2,4] : le reliquat de 1 est rééquilibré -> [4,3,2].
        var besoin = new BesoinSeances(groupe(1), matiere(1), 9, null, null, 0, "AUCUN",
                List.of(), null, null);

        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));

        assertThat(seances.stream().mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(9);
        assertThat(seances).allSatisfy(s ->
                assertThat(s.getDureeUnites()).isBetween(2, 4));
    }

    @Test
    void c05_quinzaineEmetDesSeancesSemaineAEtSemaineB() {
        var besoin = new BesoinSeances(groupe(1), matiere(1), 4, 2, null, 4, "AUCUN",
                List.of(), null, null);

        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));

        int volumeA = seances.stream().filter(s -> s.getSemaine() == SemainePlan.A)
                .mapToInt(SeancePlan::getDureeUnites).sum();
        int volumeB = seances.stream().filter(s -> s.getSemaine() == SemainePlan.B)
                .mapToInt(SeancePlan::getDureeUnites).sum();
        assertThat(volumeA).isEqualTo(4);
        assertThat(volumeB).isEqualTo(2);
        assertThat(seances).noneMatch(s -> s.getSemaine() == SemainePlan.TOUTES);
    }

    @Test
    void a04_b04_dedoublementTotalEmetDesBlocsAlignesParSousGroupe() {
        var sg1 = sousGroupe(11, 1);
        var sg2 = sousGroupe(12, 1);
        var besoin = new BesoinSeances(groupe(1), matiere(1), 4, null, null, 4, "TOTAL",
                List.of(sg1, sg2), null, null);

        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));

        // Tout le volume est émis par sous-groupe : 4 unités chacun.
        assertThat(seances.stream().filter(s -> s.getGroupe().id() == 11)
                .mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(4);
        assertThat(seances.stream().filter(s -> s.getGroupe().id() == 12)
                .mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(4);
        // Les séances de même rang partagent un blocAlignementId commun (elles seront simultanées).
        assertThat(seances).hasSize(2);
        assertThat(seances.get(0).getBlocAlignementId()).isNotNull();
        assertThat(seances.get(1).getBlocAlignementId()).isEqualTo(seances.get(0).getBlocAlignementId());
    }

    @Test
    void a04_dedoublementPartielMoitieClasseEntiereMoitieParSousGroupe() {
        var parent = groupe(1);
        var sg1 = sousGroupe(11, 1);
        var sg2 = sousGroupe(12, 1);
        var besoin = new BesoinSeances(parent, matiere(1), 8, null, null, 4, "PARTIEL",
                List.of(sg1, sg2), 77L, 42L);

        List<SeancePlan> seances = SeanceFactory.genererSeances(List.of(besoin));

        // Moitié (4 unités) en classe entière, sans bloc d'alignement.
        var entieres = seances.stream().filter(s -> s.getGroupe().id() == 1).toList();
        assertThat(entieres.stream().mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(4);
        assertThat(entieres).allSatisfy(s -> assertThat(s.getBlocAlignementId()).isNull());

        // Moitié (4 unités) par sous-groupe, blocs alignés entre sous-groupes.
        var dedoublees = seances.stream().filter(s -> s.getGroupe().parentId() != null).toList();
        assertThat(dedoublees.stream().filter(s -> s.getGroupe().id() == 11)
                .mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(4);
        assertThat(dedoublees.stream().filter(s -> s.getGroupe().id() == 12)
                .mapToInt(SeancePlan::getDureeUnites).sum()).isEqualTo(4);
        assertThat(dedoublees).allSatisfy(s -> assertThat(s.getBlocAlignementId()).isNotNull());

        // La barrette et l'affectation imposée sont propagées à toutes les séances.
        assertThat(seances).allSatisfy(s -> {
            assertThat(s.getBarretteId()).isEqualTo(77L);
            assertThat(s.getAffectationEnseignantId()).isEqualTo(42L);
        });
    }
}
