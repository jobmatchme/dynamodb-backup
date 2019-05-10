# DynamoDB Backup
This tool provides a way of making an incremental backup of your DynamoDB Table to S3  
It does so by using an AWS Lambda function listening on a DynamoDB Stream to write changes to a versioned S3 bucket as they happen  
Of course restoring from a backup is possible as well

There are four functions contained within this tool that can be used as an AWS Lambda or from the command line  
Backup is the function that writes from a DynamoDB Stream to S3 and can only be used from an AWS Lambda function currently  
Backfill is used to get existing records in the table written to S3 and is currently only available from CLI  
Snapshot allows to save the current state of the incemental S3 bucket to a single file  
Restore can write all records from a snapshot back to a table  
While restore and snapshot are available as AWS Lambda functions they are probably better used from the CLI for bigger tables due to the AWS Lambda time limit

## Usage
### Configuration
Configuration can be done two ways:
1. Environment variables
2. Config file

#### Environment Varibles
##### S3
For S3 two buckets can be configured, the bucket where the backup function stores the incremental backup and the bucket where snapshots will be stored, both can also be configured with a prefix for the objects (prefixes are shown as directories in the AWS interface)  
While you can have backup and snapshots in the same bucket you should use different prefixes for the two or new snapshots will also include old snapshots which would make them a mess from which you probably couldn't restore  
Backup and Backfill **require** the incremental bucket
Snapshot **requires** both buckets
Restore **requires** the snapshot bucket

| Variable            | Use                                                                   |
|---------------------|-----------------------------------------------------------------------|
| INCREMENTAL\_BUCKET | The bucket to store the incremental backup in                         |
| INCREMENTAL\_PREFIX | The prefix to use for objects that are part of the incremental backup |
| SNAPSHOT\_BUCKET    | The bucket to store snapshots in                                      |
| SNAPSHOT\_PREFIX    | The prefix to use for snapshots                                       |

##### DynamoDB
As Backup is an AWS Lambda function that listens on a stream it does not need DynamoDB configuration, neither does Snapshot  
Bacfill and Restore both **require** at least the region to be set, all other parameters *can* be left as default

| Variable              | Use                                                                         |
|-----------------------|-----------------------------------------------------------------------------|
| DYNAMODB\_REGION      | The AWS region the table to operate on is in                                |
| DYNAMODB\_HOST        | The host to use, defaults to the standard DynamoDB host for your AWS region |
| DYNAMODB\_PORT        | The port to connect to DynamoDB on, defaults to 443                         |
| DYNAMODB\_USETLS      | Wether to use https or not, defaults to true                                |
| DYNAMODB\_PARALLELISM | Max number of in flight requests, defaults to 32, must be a power of 2      |

#### Config file
The same rules as for configuration through environment variables apply, so here's an example config file:  
```
s3 {
  incremental {
    bucket = backup-bucket
	prefix = backup
  }
  snapshot {
    bucket = snapshot-bucket
	prefix = snapshots
  }
}

dynamodb {
  region = eu-central-1
  host = localhost
  port = 4242
  tls = false
  parallelism = 16
}
```

### CLI
It will be assumed that the compiled .jar file is in your current directory and that it is named `dynamodb-backup.jar`
To use a config file replace `java` with `java -Dconfig.file=path/to/your.conf` in the following

#### Backfill
To run the Backfill function run the command `java -jar dynamodb-backup.jar --backfill <table name>` where `<table name>` is the name of the table you want to back up

#### Snapshot
To run the Snapshot function run the command `java -jar dynamodb-backup.jar --snapshot`

#### Restore
Restore does not create or empty the table for you, all it does is insert all records in a snapshot into a specified table, so you should make sure that your table is prepared for the restore  
To run the Restore function run the command `java -jar dynamodb-backup.jar --restore <table name> <snapshot>` where `<table name>` is the table you want to restore the snapshot to and `<snapshot>` is the file name of the snapshot in the snapshot bucket without the prefix

### AWS Lambda
To use these functions as AWS Lambdas create a function with the Java 8 runtime and upload the compiled .jar file to it

#### Backup
The Backup function is available to an AWS Lambda as `eu.jobmatchme.dynamodb.backup.Lambda::backup`  
After setting it up you'll need to go to the DynamoDB interface, select your table and go to the `Triggers` tab, there you can then create a trigger using this function
Once done, it will automatically run whenever something changes in the table

#### Snapshot
The Backup function is available to an AWS Lambda as `eu.jobmatchme.dynamodb.backup.Lambda::snapshot`
After setting it up you can trigger it by making it receive an API Gateway Proxy Request Event, any route is fine, method and body also don't matter

#### Restore
The Backup function is available to an AWS Lambda as `eu.jobmatchme.dynamodb.backup.Lambda::restore`
After setting it up you can trigger it by making it receive an API Gateway Proxy Request Event, any route is fine
You'll have to specify the table name and snapshot file in a JSON object sent as request body as `tableName` and `snapshotFilename` respectively

## Building
To build make sure sbt is available on your system, then run `sbt assembly` in the repository root, this will build a .jar file and place it in `target/scala-2.12/`, it should usually be named `dynamodb-backup.jar`  
The built .jar file contains all AWS Lambda functions as well as the CLI interface
