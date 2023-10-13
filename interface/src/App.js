import logo from './logo.svg';
import './App.css';
import { useState, useEffect } from 'react';

function CreateSprintButton(props) {
    const createSprint = () => {
        fetch("http://localhost:8080/sprints", {method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: 'Sprint Test', current: true })})
            .then(res => res.json())
            .then(json => {
                props.setSprint(json);

                fetch("http://localhost:8080/tasks", {method: 'POST'}).then(res => {
                fetch("http://localhost:8080/sprints/" + json.id + "/tasks", {method: 'POST'})
                    .then(res => res.json())
                    .then(json => {
                        props.setSprint(json);
                    });
                });
            });
    };

    if(props.sprint != null) {
        return null;
    }

    return (<button
        onClick={createSprint}
        title="Learn More"
        color="#841584"
    >Create Sprint</button>);
}

function SyncWithTrelloButton(props) {
    const syncWithTrello = () => {
        fetch("http://localhost:8080/tasks", {method: 'POST'});
    };

    return (<button
        onClick={syncWithTrello}
        title="Learn More"
        color="#841584"
    >Sync with Trello</button>);
}

function App() {
  const [sprint, setSprint] = useState([]);

  const hasCurrentSprint = () => {
      return sprint != null;
  }

  useEffect(() => {
    fetch("http://localhost:8080/sprints/current", {method: 'GET'})
        .then(res => {
          if(!res.ok) {
            throw new Error("No current sprint")
          }

          return res.json();
        }).catch(error => {})
        .then(json => setSprint(json))
  }, []);

  return (
    <div className="App">
        <SyncWithTrelloButton></SyncWithTrelloButton>
        <CreateSprintButton sprint={sprint} setSprint={setSprint}></CreateSprintButton>

      {JSON.stringify(sprint, null, 2)}
    </div>
  );
}

export default App;
