import logo from './logo.svg';
import './App.css';
import { useState, useEffect } from 'react';
import React from 'react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
);

export const options = {
    responsive: true,
    plugins: {
        legend: {
            display: false
        },
        title: {
            display: true,
            text: 'Chart.js Line Chart',
        },
    },
};

const labels = ['January', 'February', 'March', 'April', 'May', 'June', 'July'];

export const data = {
    labels,
    datasets: [
        {
            label: 'Dataset 1',
            data: labels.map(() => Math.floor(Math.random() * 10)),
            borderColor: 'rgb(255, 99, 132)',
            backgroundColor: 'rgba(255, 99, 132, 0.5)',
        },
        {
            label: 'Dataset 2',
            data: labels.map(() => Math.floor(Math.random() * 10)),
            borderColor: 'rgb(53, 162, 235)',
            backgroundColor: 'rgba(53, 162, 235, 0.5)',
        },
    ],
};

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

        <Line options={options} data={data} />
    </div>
  );
}

export default App;
