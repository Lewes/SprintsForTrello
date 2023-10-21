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

function getSprintProgress(sprintId) {
    return fetch("http://localhost:8080/sprints/" + sprintId + "/progress", {method: 'GET'});
}

function BurndownChart(props) {
    const [progress, setProgress] = useState(data);

    useEffect(() => {
        if(props.sprint == null ||
            props.sprint.id == null ||
            props.sprint.status !== "IN_PROGRESS") {
            return;
        }

        getSprintProgress(props.sprint.id)
            .then(res => {
                if(!res.ok) {
                    throw new Error("No current sprint")
                }

                return res.json();
            })
            .catch(() => {
                return null;
            })
            .then(json => {
                if(json == null) {
                    return null;
                }

                const labels = Object.keys(json.days2ExpectedPoints)

                setProgress({
                    labels,
                    datasets: [
                        {
                            label: 'Expected Burndown',
                            data: Object.keys(json.days2ExpectedPoints).map(key => json.days2ExpectedPoints[key]),
                            borderColor: 'rgb(178,178,178)',
                            backgroundColor: 'rgba(169,169,169,0.5)',
                        },
                        {
                            label: 'Actual Burndown',
                            data: Object.keys(json.days2RemainingPoints).map(key => json.days2RemainingPoints[key]),
                            borderColor: 'rgb(115,99,255)',
                            backgroundColor: 'rgba(99,125,255,0.5)',
                        }
                    ],
                });
            });
    }, [props.sprint]);

    if(props.sprint == null ||
        props.sprint.id == null ||
        props.sprint.status !== "IN_PROGRESS") {
        return null;
    }

    return (
        <div className="BurndownChart">
            <Line options={options} data={progress} />
        </div>
    );
}

export default BurndownChart;