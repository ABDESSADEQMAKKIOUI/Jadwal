package ma.jadwal.enseignant.depot;

import ma.jadwal.enseignant.entite.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {

    List<Habilitation> findByEnseignantId(Long enseignantId);

    void deleteByEnseignantId(Long enseignantId);
}
