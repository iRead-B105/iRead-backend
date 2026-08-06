CREATE TABLE qa_demo_dataset_deployments (
    dataset_key varchar(64) NOT NULL,
    backend_tag varchar(128) NOT NULL,
    applied_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_QA_DEMO_DATASET_DEPLOYMENTS PRIMARY KEY (dataset_key)
);
