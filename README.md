# Deprecated notice

**This application has now been replaced by [salesforce-event-bus](https://github.com/guardian/support-service-lambdas/tree/main/handlers/salesforce-event-bus) which is now relaying messages between Salesforce and membership-workflow. **

# salesforce-message-handler

Salesforce is configured to call this API when a contact update modifies a relevant field (check the salesforce outbound message called **CEX Address Change_Fulfilment** for the specific triggering rules).

The incoming messages from salesforce are in XML format and contain a list of ids of recently modified contacts. 

This app parses the received XML and for each contact Id it puts a json message in an sqs queue called **salesforce-outbound-messages-[STAGE]**.

Messages in this queue will be picked up by [membership workflow](https://github.com/guardian/membership-workflow) to update the related zuora and identity records accordingly.

