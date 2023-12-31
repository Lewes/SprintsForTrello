import './App.css';
import { useState, useEffect } from 'react';
import BurndownChart from "./BurndownChart";
import Tasks from "./Tasks.js";

function addTasksToSprint(sprintId) {
    return fetch("http://localhost:8080/sprints/" + sprintId + "/tasks", {method: 'POST'});
}

function syncTasksFromTrello() {
    return fetch("http://localhost:8080/tasks", {method: 'POST'});
}

function CreateSprintButton(props) {
    const createSprint = () => {
        fetch("http://localhost:8080/sprints", {method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: 'Sprint Test', current: true })})
            .then(res => res.json())
            .then(json => {
                props.setSprint(json);

                syncTasksFromTrello().then(() => addTasksToSprint(json.id));
            });
    };

    return (<button
        onClick={createSprint}
        title="Learn More"
        color="#841584"
    >Create Sprint</button>);
}

function StartSprint(props) {
    const startSprint = () => {
        fetch("http://localhost:8080/sprints/" + props.sprint.id, {method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: "IN_PROGRESS", current: true })})
            .then(res => res.json())
            .then(json => {
                props.setSprint(json);
            });
    };

    return (<button
        onClick={startSprint}
        title="Learn More"
        color="#841584"
    >Start Sprint</button>);
}

function getCurrentSprint() {
    return fetch("http://localhost:8080/sprints/current", {method: 'GET'});
}

function addTasksToCurrentSprint() {
    return syncTasksFromTrello("current");
}

function SyncWithTrelloButton(props) {
    const syncWithTrello = () => {
        syncTasksFromTrello().then(() => {
            addTasksToCurrentSprint().then(() => {
                getCurrentSprint().then(res => {
                    if (!res.ok) {
                        throw new Error("No current sprint")
                    }

                    return res.json();
                }).catch(() => {}).then(json => props.setSprint(json))
            });
        });
    };

    return (<button
        onClick={syncWithTrello}
        title="Learn More"
        color="#841584"
    >Sync with Trello</button>);
}

function NoSprintSelectedButtons(props) {
    if(props.sprint != null &&
        props.sprint.id != null) {
        return;
    }

    return (
        <CreateSprintButton setSprint={props.setSprint}></CreateSprintButton>
    )
}

function SprintSelectedButNotStarted(props) {
    if(props.sprint == null ||
        props.sprint.id == null ||
        props.sprint.status !== "PLANNING") {
        return;
    }

    return (
        <StartSprint sprint={props.sprint} setSprint={props.setSprint}></StartSprint>
    )
}

function SprintSelectedAndInProgress(props) {
    if(props.sprint == null ||
        props.sprint.id == null) {
        return;
    }

    return (
        <SyncWithTrelloButton setSprint={props.setSprint} ></SyncWithTrelloButton>
    )
}

function App() {
  const [sprint, setSprint] = useState([]);

  useEffect(() => {
    getCurrentSprint().then(res => {
        if(!res.ok) {
            throw new Error("No current sprint")
        }

        return res.json();
    }).catch(() => {}).then(json => setSprint(json))
  }, []);

  return (
    <div className="App">
        <h1>Sprints for Trello</h1>
        <div className="Container">
            <div className="Buttons">
                <NoSprintSelectedButtons sprint={sprint} setSprint={setSprint} ></NoSprintSelectedButtons>
                <SprintSelectedButNotStarted sprint={sprint} setSprint={setSprint} ></SprintSelectedButNotStarted>
                <SprintSelectedAndInProgress sprint={sprint} setSprint={setSprint} ></SprintSelectedAndInProgress>
            </div>

            <BurndownChart sprint={sprint}  />
            <Tasks sprint={sprint}></Tasks>
        </div>
    </div>
  );
}

export default App;
