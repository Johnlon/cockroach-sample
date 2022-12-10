# cockroach-sample

This repo contains the complete code and binary I used to evaluate refreshing materialised views in cockroach in order to investigate that problem.

I did this because an earlier attempt failed with the error below and despite all attempts to turn off the "explicit transaction" and even reading the JDBC driver code that I had I wasn't able to fix it with the code I had at the time.

The error was ...

```
error : cannot refresh view in an explicit transaction
```

Having run this new demo in Dec 2022 I have proved that the function does actually work today with the latest cockroach and JDBC driver.
The has this led me to look into the server code for cockroach where I discovered the issue.

The issue is that the version of cockroach I had originally used from around Jul 2021 had a bug in it that was fixed 9 months later in Apr 2022. The git references are below.


# Using demo

The cockroach build tested here and working is CCL v22.2.0 @ 2022/12/05 16:37:36 (go1.19.1)

- Unzip the cluster program with : gunzip client.gz
- Start local cluster with : ./client start-single-node --insecure


The java program tested is App.java and I used Java 11 and the driver postgresql-42.5.1.jar

- Build and run

```sh
$ javac -cp postgresql-42.5.1.jar App.java
$ java -cp .:postgresql-42.5.1.jar App
DROP MATERIALIZED VIEW IF EXISTS vw
DROP TABLE IF EXISTS tab
CREATE TABLE IF NOT EXISTS tab (a STRING)
INSERT INTO tab(a) VALUES('before-create-view')
CREATE MATERIALIZED VIEW IF NOT EXISTS VW AS SELECT a FROM tab
INSERT INTO tab(a) VALUES('after-create-view')
REFRESH MATERIALIZED VIEW VW
SELECT * FROM vw
 before-create-view
 after-create-view
```


# Github history showing the bug and what happened next

Commit showing this error message is https://github.com/cockroachdb/cockroach/blob/aaea705e5ff9d798a410036b4e55e887d917a461/pkg/sql/refresh_materialized_view.go

See file history : https://github.com/cockroachdb/cockroach/commits/fcf22cdf10b7ee872417c3b7d8c194d37e17c7ef/pkg/sql/refresh_materialized_view.go

```
// July 6 2021
if !p.EvalContext().TxnImplicit {
		return nil, pgerror.Newf(pgcode.InvalidTransactionState, "cannot refresh view in an explicit transaction")
}
```

Code has been refactored and different checks and is not looking for implicit transactions anymore but instead a single statement txn.
https://github.com/cockroachdb/cockroach/blob/fcf22cdf10b7ee872417c3b7d8c194d37e17c7ef/pkg/sql/refresh_materialized_view.go

```
// Sep 1 2022
if !params.p.extendedEvalCtx.TxnIsSingleStmt {
		return pgerror.Newf(pgcode.InvalidTransactionState, "cannot refresh view in a multi-statement transaction")
}
```

The next commit changes the logic "sql: block operations properly on implicit transaction".  https://github.com/cockroachdb/cockroach/commit/690f6978a47a6d92cef9cb64d3043f9ef49f2f54
.. and the comment is ....

```
Previously, an enhancement was made to allow implicit
transactions to execute multiple statements, which caused
code to block operations in implicit transactions to break.
This was problematic because these scenarios cared that
only a single statement is executed, and not if it was
only an implicit transition. For example, the declarative
schema changer and legacy schema changer could be mixed
in implicit transactions after this change. To address this,
this patch modifies places that we're checking for implicit
transactions to instead check for single statement transactions
```

_Fix is tagged in builds..._

- v23.1.0-alpha.00000000
- v22.2.0
- v22.2.0-rc.3
- v22.2.0-rc.2
- v22.2.0-rc.1
- v22.2.0-beta.5
- v22.2.0-beta.4
- v22.2.0-beta.3
- v22.2.0-beta.2
- v22.2.0-beta.1
- v22.2.0-alpha.4
- v22.2.0-alpha.3
- v22.2.0-alpha.2
- v22.2.0-alpha.1
- v22.2.0-alpha.00000000
- custombuild-v22.2.0-beta.5-59-g3a8bf9fd51f
- custombuild-v22.2.0-alpha.3-1315-ge956611336
- custombuild-v22.2.0-alpha.3-1315-gc941dfa066
- custombuild-v22.2.0-alpha.3-1315-g96a1c6a3ef
- custombuild-v22.2.0-alpha.3-1315-g94fe24a5b0
- custombuild-v22.2.0-alpha.3-1315-g24e8baa987
- custombuild-v22.2.0-alpha.3-1315-g2eb734daf2
- custombuild-v22.2.0-alpha.3-1074-g3d64ee63e5
- custombuild-v22.2.0-alpha.3-1000-gbdbe1707eb0
- custombuild-v22.2.0-alpha.3-1000-g4f525537322
- @cockroacklabs/cluster-ui@22.2.0-prerelease-3
- @cockroacklabs/cluster-ui@22.2.0-prerelease-2
- @cockroacklabs/cluster-ui@22.2.0-prelease-1
- @cockroachlabs/cluster-ui@23.1.0-prerelease.0
- @cockroachlabs/cluster-ui@22.2.0
- @cockroachlabs/cluster-ui@22.2.0-prerelease-8
- @cockroachlabs/cluster-ui@22.2.0-prerelease-7
- @cockroachlabs/cluster-ui@22.2.0-prerelease-6
- @cockroachlabs/cluster-ui@22.2.0-prerelease-5
- @cockroachlabs/cluster@22.2.0-prerelease-4

# Other refs

https://stackoverflow.com/questions/51073697/refresh-materialized-view-does-not-work-using-jdbc#comment131925227_51073697

https://stackoverflow.com/questions/44229510/how-to-execute-an-sql-query-without-starting-a-transaction-with-postgresql-jdbc


https://github.com/search?q=refresh+view+%22explicit+transaction%22&type=code


fork of source: https://github.com/fabianlindfors/cockroach/blob/7004f6bded2f8ef74d32440999a3fbed0353f63d/pkg/sql/refresh_materialized_view.go

This link discusses the client starting a tran even when autocommit is off: 
https://franckpachot.medium.com/transaction-management-in-postgresql-and-what-is-different-from-oracle-eeae34675a77


Adventures in JDBC and the PostgreSQL JDBC driver : https://weinan.io/2017/05/21/jdbc-part5.html - This page also discusses how to turn on logging so you can see the protocol including begins/commits
