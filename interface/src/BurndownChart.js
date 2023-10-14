import React, {useEffect, useState} from 'react';
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

const options = {
    responsive: true,
    plugins: {
        legend: {
            display: false
        },
        title: {
            display: true,
            text: 'Burndown Chart',
        },
    },
};

const data = {
    labels: [],
    datasets: [{}],
};

function BurndownChart(props) {
    const [remainingTasksData, setRemainingTasks] = useState(data);

    useEffect(() => {
        if(props.sprint == null ||
            props.sprint.id == null) {
            return;
        }

        fetch("http://localhost:8080/sprints/" + props.sprint.id + "/progress", {method: 'GET'})
            .then(res => {
                if(!res.ok) {
                    throw new Error("No current sprint")
                }

                return res.json();
            })
            .catch(error => {
                return null;
            })
            .then(json => {
                if(json == null) {
                    return null;
                }

                const labels = Object.keys(json.days2ExpectedPoints)

                setRemainingTasks({
                    labels,
                    datasets: [
                        {
                            label: 'Expected Burndown',
                            data: Object.keys(json.days2ExpectedPoints).map(key => json.days2ExpectedPoints[key]),
                            borderColor: 'rgb(255, 99, 132)',
                            backgroundColor: 'rgba(255, 99, 132, 0.5)',
                        },
                        {
                            label: 'Actual Burndown',
                            data: Object.keys(json.days2RemainingPoints).map(key => json.days2RemainingPoints[key]),
                            borderColor: 'rgb(255, 99, 132)',
                            backgroundColor: 'rgba(255, 99, 132, 0.5)',
                        }
                    ],
                });
            });
    }, [props.sprint]);

    if(props.sprint == null ||
        props.sprint.id == null) {
        return null;
    }

    return (
        <Line options={options} data={remainingTasksData} />
    );
}

export default BurndownChart;