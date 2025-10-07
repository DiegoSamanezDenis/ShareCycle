## changes made : 
rename the sql files in src/main/resources/db/migration/V2_create_users.sql ->  V2__create_users.sql 
	it has a double underscore after V2 which is the convention for flyway to run it for docker, otherwise it gets ignored 