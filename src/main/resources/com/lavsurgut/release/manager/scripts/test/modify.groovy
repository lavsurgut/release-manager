
import com.lavsurgut.release.manager.lib.task.SqlPlusTask
import com.lavsurgut.release.manager.lib.task.Task
import groovy.sql.Sql

taskName = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getName()
taskPath = new File(getClass().protectionDomain.codeSource.location.path).getParent() + "/"

log.info "Configuring " + taskName + " ..."

log.debug db

sql = new Sql(testSource)

return new SqlPlusTask(user : "${relman_user}"
					 , password : "${relman_user_pass}"
					 , tnsName : "${db}"
					 , script : taskPath + "test.sql"
					 )
/*

package com.lavsurgut.release.manager.scripts

import com.lavsurgut.release.manager.lib.task.SqlPlusTask
import com.lavsurgut.release.manager.lib.task.ParallelTask
import com.lavsurgut.release.manager.lib.task.Task
import groovy.sql.Sql

taskName = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getName()
taskPath = new File(getClass().protectionDomain.codeSource.location.path).getParent() + "/"

log.info "Configuring " + taskName + " ..."


sql = new Sql(lemStgDataSource)
dboSql = new Sql(lemDboDataSource)

Map tasks =
		["Task1" : new SqlPlusTask(user : "${lem_stg_user}"
			, password : "${lem_stg_user_pass}"
			, tnsName : "${lem_db}"
			, script : taskPath + "test2.sql"
			, executeBeforeChecks: {
				def res2 = dboSql.firstRow("select count(1) cnt from lem_dbo.city")
				assert (res2.cnt == 20) 
			})
		
		,"Task2" : new SqlPlusTask(user : "${lem_stg_user}"
			, password : "${lem_stg_user_pass}"
			, tnsName : "${lem_db}"
			, script : taskPath + "test.sql")]

return new ParallelTask(runnables: tasks
					,executeBeforeChecks: {
				def res2 = dboSql.firstRow("select count(1) cnt from lem_dbo.city")
				assert (res2.cnt == 20) 
			})
*/


