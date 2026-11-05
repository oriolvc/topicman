-- Records that the example data was offered to this database, so it loads at most once and deleted
-- examples do not come back. At most one row.
CREATE TABLE example_data_load (
    id        INTEGER                  PRIMARY KEY,
    loaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
