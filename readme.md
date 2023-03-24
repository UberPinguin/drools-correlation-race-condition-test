# Simplified test case for correlating multiple Events using a tracking fact

This test case sets up a small KieSession with some rules and fact definitions.
It then inserts a series of events which should be tracked by a fact in working memory.

DeviceOffline events are added to a Set in the tracking fact.
DeviceAdopted events remove their devices from that Set.
When the Set becomes empty, the tracking fact is deleted.