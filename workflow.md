# Baseline: Iteration planning, design and implementation discussion

During the iteration team designates one of them as the [iteration master](#master), responsible for the coordination and planning of the iteration.

Iteration consists of three sprints:
- [feature sprint](#featuresprint) dedicated to implementing new functionalities.
- [QA testing period](#qaperiod), during which no new functionalities are added to the product, team finds and tracks bugs and developers have some extra time for loosely related work.
- [bug-fixing sprint](#bugsprint) dedicated to fixing regression, taking at most half of the time of the original feature sprint.

Feature and bug-fixing sprints are started with respective planning sessions.
Iteration is supposed to end with an increment of a production-release quality.
At the end of each iteration team conducts a retrospective meeting, during which they discuss changes to the workflow and name a new [iteration master](#master).

## <a name="featuresprint"> feature-sprint planning session.
Team starts an iteration with a planning session.
[iteration master](#master) is a person responsible for its course and for arranging the [kanban board](#kanban) with the issue the team decided to take on during this iteration.
Goals of the session:
  - to determine which issues will be worked on
  - to rate the perceived priority of the issues, defined by the *Must Have*, *Should Have* and *Could Have* labels.
  - developers should try to compose the scope that consist in ~ 60% of the *Must Have* issues, and in the remaining 40% of *Should Have* and *Could Have* issues
  <!-- , which can be traded for [unplanned issues](#unplanned) in case of emergencies. -->
  - to determine the length of the sprint (default 2 weeks, but possibly longer)

## <a name="qaperiod"> After the feature sprint
Team enters a mandatory intensive QA testing period of about *one week*
  - during this period no new features are deployed to the QA!
  - ops team does the QA testing and files new issues
  - bugs are collected into the *to-be-planned* column of the [kanban board](#kanban)
  - developers can simultanously work on separate librararies / proof-of-concepts / other projects or simply do some learning, not directly connected to the product

## <a name="bugsprint"> Bug-fixing sprint planning session
  - as before [iteration master](#master) is responsible for coordinating the planning session.
  - this sprint is dedicated solely to bug fixing, no new features are added to the scope or deployed to the QA!
  - as a baseline bug-fixing sprint should be around *half of the time* of its respective feature sprint.
  - [iteration master](#master) sets a provisional date for the deployment, by which time all the work ceases and the team coordinates their effort towards deploying the next version of the product.

## After the bug-fixing sprint
 - one day dedicated for production deployment
 - [iteration master](#master) should try to set the date for beginning of the week
 - after the deployment [iteration master](#master) sets the date for a [retrospective meeting](#retro).

# <a name="unplanned"> Unplanned issues </a>

Unplanned issues are just taken on by the developers, if needed the sprint is prolonged.
They are put into a special *Fast-track* column of the [kanban board](#kanban) by the [iteration master](#master).

<!-- *Could Have*, and then if necessary *Should Have* issues may be traded out of scope in an emergency. -->
<!-- This is done following a quick slack discussion between the developers: -->

<!-- - who takes the issue? -->
<!-- - how much is it worth? -->
<!-- - which issue is it traded for? -->

<!-- Unplanned issues get a special *fast track* label assigned by the [iteration master](#master), who is also responsible for tracking their progress. -->
<!-- They don't need to strictly follow the left-to-right column order in the [kanban board](#kanban). -->

<!-- No unplanned issues can be claimed or added to planning after the fact! -->

<!-- --- -->
<!-- **NOTE** -->

<!-- No-one should force unplanned work onto the development team, not even members of the team themselves! -->
<!-- If someone requests that an unplanned, urgent, and critical piece of work must be attained to, but there is nothing that can be traded out of scope for the iteration, the current sprint is cancelled, and after finishing the request -->
<!-- a new planning session must be scheduled by the current [iteration master](#master). -->

<!-- --- -->

# <a name="master"> Iteration master </a>

Iteration master is the person responsible for organizing and running the planning session, physically arranging the issue on the [kanban board](#kanban), as well as moving them between the columns, planning any meetings or calendar remainders for the whole team during the duration of the iteration.

To start with we will simply assign the *iteration master* in a round-robin fashion, changing between team members after every completed iteration.
Any person from the team can be name as the iteration master, not exclusively a developer.

# <a name="kanban"> kanban board </a>

Priority and status of issues should be reflected in a [github kanban board](https://github.com/district0x/memefactory/projects/1), maintained during the iteration by the [iteration master](#master).
Typically issues enter at the left of the board and during the progress they are moved from left to right between the columns, with the exception of [unplanned issues](#unplanned).

The colums are:
- *To be planned* : issues that need to be discussed during a planning session
- *Backlog* : issues that will be worked on during the iteration
- *Todo* : issues taken by developers
- *Done* : finished issue, according to the [definition of done](#done).
- *Fast-track* : [unplanned issues](#unplanned) issues that emerged during the sprint.

After issue is [done](#done) it remains on the board until the beginning of a next [feature planning session](#featuresprint), when the board is cleaned by the new [iteration master](#master).

# <a name="done"> Definition of done </a>

<!-- subject to change -->
1. Code Complete
- code adheres to the clearly defined completion criteria specified in its ticket
- all *TODO* annotations have been resolved
- code has been Peer Reviewed and accepted into the main branch

2. Environment complete
- Continuous Integration is configured to schedule a build on check-in
- product should be shown to work on all required platforms / browsers

3. Test complete
- all the test cases should have been executed and the increment proven to work as expected
- manual QA exploratory testing has been performed, and a test report should have been generated
- Regression testing has been completed, and the functionality provided in previous iterations has been shown to still work

# <a name="retro"> Retrospective meeting </a>

This methodology is supposed to be open-ended and constantly evolving to best suit current needs.
After the succesfull deployment [iteration master](#master) sets up and coordinates a meeting with the following goals:
 - whole team is welcome to attend the meeting and express their opinion
 - team reflects on how to become more effective in the future
 - team members propose changes to the workflow
 - [iteration master](#master) creates a list of proposed improvements and makes agreed adjustments, if any, to this document.
 - team determines the person responsible for planning the next iteration, [iteration master](#master).

*Do's*
 - evaluate how the last iteration went, focusing specifically around the team dynamics and the workflow
 - it is OK to articulate and let go of frustrations with the process, team etc
 - successful retrospective meeting results in a *list of improvements* that team members take ownership of and work towards in the next iteration.

*Dont's*
 - meeting shouldn't take more than 1 hour
 - focus not on what was delivered, but rather on how the team worked together to make it happen
