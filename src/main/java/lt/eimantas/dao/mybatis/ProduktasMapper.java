package lt.eimantas.dao.mybatis;

import org.mybatis.cdi.Mapper;
import java.util.List;

@Mapper
public interface ProduktasMapper {
    List<ProduktasModel> findAll();
    void insert(ProduktasModel produktas);
}
