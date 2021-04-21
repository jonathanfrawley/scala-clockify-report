# Prerequisites
 - [SBT](https://www.scala-sbt.org/1.x/docs/Setup.html)

# Example Usage
    export API_KEY="<API_KEY>"
    export WORKSPACE_ID="<CLOCKIFY_WORKSPACE_ID>"

    export START_DATE=`date -I --date="21 days ago"`
    export END_DATE=`date -I`

    sbt run
