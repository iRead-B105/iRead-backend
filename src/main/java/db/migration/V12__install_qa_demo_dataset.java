package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class V12__install_qa_demo_dataset extends BaseJavaMigration {

    private static final String DATASET_RESOURCE = "db/demo-data/qa-demo-reset.sql";

    @Override
    public void migrate(Context context) {
        ScriptUtils.executeSqlScript(
                context.getConnection(),
                new ClassPathResource(DATASET_RESOURCE)
        );
    }
}
