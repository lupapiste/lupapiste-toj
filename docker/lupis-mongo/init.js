db.createUser({
  user: "toj",
  pwd: "toj",
  roles: [{role: "readWriteAnyDatabase", db: "admin"},
          {role: "dbAdminAnyDatabase", db: "admin"},
          {role: "readWrite", db: "toj"}]});
