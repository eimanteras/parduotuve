package lt.eimantas.dao.mybatis;

import org.mybatis.cdi.Mapper;
import java.util.List;

@Mapper
public interface SandelisMapper {
    List<SandelisModel> findAll();
    void insert(SandelisModel sandelis);
}

