package de.neebs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryViaSql {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> query(String query) {
        return jdbcTemplate.query(query, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet resultSet, int row) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    if (metaData.getColumnTypeName(i + 1).equals("DATE")) {
                        map.put(metaData.getColumnName(i + 1).toLowerCase(), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(resultSet.getDate(i + 1)));
                    } else {
                        map.put(metaData.getColumnName(i + 1).toLowerCase(), resultSet.getObject(i + 1));
                    }
                }
                return map;
            }
        });
    }
}
