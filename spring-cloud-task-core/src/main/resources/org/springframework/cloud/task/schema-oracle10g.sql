
CREATE TABLE TASK_EXECUTION  (
	TASK_EXECUTION_ID VARCHAR2(100)  NOT NULL PRIMARY KEY ,
	START_TIME TIMESTAMP DEFAULT NULL ,
	END_TIME TIMESTAMP DEFAULT NULL ,
	TASK_NAME  VARCHAR2(100) ,
	EXIT_CODE INTEGER ,
	EXIT_MESSAGE VARCHAR2(2500) ,
	LAST_UPDATED TIMESTAMP ,
	STATUS_CODE VARCHAR2(10)
);

CREATE TABLE TASK_EXECUTION_PARAMS  (
	TASK_EXECUTION_ID VARCHAR2(100) NOT NULL ,
	TASK_PARAM VARCHAR2(250) ,
	constraint TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)
	references TASK_EXECUTION(TASK_EXECUTION_ID)
) ;
