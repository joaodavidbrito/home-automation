const loadFormData = () => {
    document.querySelector('input[name="chartTitle"').value = localStorage.getItem('chart_title')
    document.querySelector('#subtitle').value = localStorage.getItem('subtitle')
    document.querySelector('select[name="chart"').value = localStorage.getItem('chart')
    document.querySelector('select[name="suit"').value = localStorage.getItem('suit')
    document.querySelector('input[name="time1"').value = localStorage.getItem('time1')
    document.querySelector('#timestamp1').value = localStorage.getItem('timestamp1')
    document.querySelector('input[name="time2"').value = localStorage.getItem('time2')
    document.querySelector('#timestamp2').value = localStorage.getItem('timestamp2')
    document.querySelector('input[name="hAxisTitle"').value = localStorage.getItem('hTitle')
    document.querySelector('input[name="hAxisMin"').value = localStorage.getItem('hMin')
    document.querySelector('input[name="hAxisMax"').value = localStorage.getItem('hMax')
    document.querySelector('input[name="vAxisTitle"').value = localStorage.getItem('vTitle')
    document.querySelector('input[name="vAxisMin"').value = localStorage.getItem('vMin')
    document.querySelector('input[name="vAxisMax"').value = localStorage.getItem('vMax')
    document.querySelector('input[name="nf"').checked = localStorage.getItem('nf') === 'true'
    document.querySelector('#trendline').checked = localStorage.getItem('trendline') === 'true'
}

const init = () => {
    document.querySelector('#chart_div').style.display = 'none'
    document.querySelector('#chart_div_histo').style.display = 'none'
    document.querySelector('#summary').style.display = 'none'
    document.querySelector('#piechart_3d').style.display = 'none'
}

init()

loadFormData()
const form = document.querySelector('form')
const back = document.querySelector('#back')
const cancelBtn = document.querySelector('#cancel')

const t1 = document.querySelector('input[name="time1"]')
const t2 = document.querySelector('input[name="time2"]')
const ts1 = document.getElementById('timestamp1')
const ts2 = document.getElementById('timestamp2')

let subtitle
let title

form.addEventListener('submit', (ev) => {
    ev.preventDefault();

    subtitle = document.getElementById('subtitle').value ? document.getElementById('subtitle').value : (document.querySelector('#suit').value ? document.querySelector('#suit').value : '')
    title = document.querySelector('input[name="chartTitle"]').value === '' ? document.querySelector('select[name="chart"').selectedOptions[0].innerHTML : document.querySelector('input[name="chartTitle"]').value
    title += subtitle ? `\n(${subtitle})` : ''

    ts1.value = new Date(t1.value).getTime()
    ts2.value = new Date(t2.value).getTime()

    if (form.checkValidity() === false) {
        event.stopPropagation();
    }
    form.classList.add('was-validated');

    document.querySelector('#chart_div').style.display = ''
    document.querySelector('#piechart_3d').style.display = ''
    saveData()
    sendData()
})

back.addEventListener('click', (ev) => {
    window.location = 'http://localhost:3000'
})

cancelBtn.addEventListener('click', (ev) => {
    location.reload()
})

ts1.addEventListener('input', (e) => {
    t1.value = moment(parseInt(ts1.value)).format("YYYY-MM-DDTHH:mm:ss.SSS")
})

ts2.addEventListener('input', (e) => {
    t2.value = moment(parseInt(ts2.value)).format("YYYY-MM-DDTHH:mm:ss.SSS")
})



t1.addEventListener('input', (e) => {
    ts1.value = new Date(t1.value).getTime()
})

t2.addEventListener('input ', (e) => {
    ts2.value = new Date(t2.value).getTime()
})


let resp = []
const sendData = () => {
    const formData = new FormData(form)
    let object = {}
    formData.forEach((value, key) => { object[key] = value });
    const json = JSON.stringify(object);

    const fetchData = async () => {
        const resp = await fetch('/charts_fetch_data', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: json
        })
        if (resp.status === 200) {
            return resp.json()
        } else {
            throw 'OH NO! Something wrong happened while fetching the data'
        }
    }

    fetchData().then((resp) => {
        const nf = document.querySelector('input[name="nf"]').checked

        const data = resp.data
        const table = resp.table

        // console.log(data);
        // console.log(table);

        drawChart(data)
        if (nf) {
            document.querySelector('#chart_div_histo').style.display = 'block'
            const footer = document.querySelector('footer')
            drawHistoChart(data).then(() => {
                // do something after histo renders
            })
            document.querySelector('#nfh').style.display = ''
            document.querySelector('#notfounds').style.display = ''
        } else {
            document.querySelector('#chart_div_histo').innerHTML = ''
            document.querySelector('#chart_div_histo').style.display = 'none'
            document.querySelector('#nfh').style.display = 'none'
            document.querySelector('#notfounds').style.display = 'none'
        }

        document.querySelector('#summary').style.display = ''
        document.querySelector('#total').innerHTML = table.total
        document.querySelector('#min').innerHTML = table.min
        document.querySelector('#q1').innerHTML = table.q1
        document.querySelector('#q2').innerHTML = table.q2
        document.querySelector('#q3').innerHTML = table.q3
        document.querySelector('#max').innerHTML = table.max
        document.querySelector('#mean').innerHTML = table.average ? Math.round(table.average.toFixed(2)) : null
        document.querySelector('#stddev').innerHTML = table.stdDev ? Math.round(table.stdDev.toFixed(2)) : null
        document.querySelector('#outliers').innerHTML = table.outliers
        document.querySelector('#perc').innerHTML = table.outliersPerc ? Math.round(table.outliersPerc.toFixed(2)): null
        document.querySelector('#notfounds').innerHTML = table.nf
        document.querySelector('#inf').innerHTML = table.inf
        document.querySelector('#sup').innerHTML = table.sup
        drawPieChart(table)

    }).catch(err => {
        console.log(err)
    })
}

const saveData = () => {
    localStorage.setItem('chart_title', document.querySelector('input[name="chartTitle"]').value)
    localStorage.setItem('subtitle', document.querySelector('#subtitle').value)
    localStorage.setItem('chart', document.querySelector('select[name="chart"]').value)
    localStorage.setItem('suit', document.querySelector('select[name="suit"]').value)
    localStorage.setItem('time1', document.querySelector('input[name="time1"]').value)
    localStorage.setItem('timestamp1', document.querySelector('#timestamp1').value)
    localStorage.setItem('time2', document.querySelector('input[name="time2"]').value)
    localStorage.setItem('timestamp2', document.querySelector('#timestamp2').value)
    localStorage.setItem('hTitle', document.querySelector('input[name="hAxisTitle"]').value)
    localStorage.setItem('hMin', document.querySelector('input[name="hAxisMin"]').value)
    localStorage.setItem('hMax', document.querySelector('input[name="hAxisMax"]').value)
    localStorage.setItem('vTitle', document.querySelector('input[name="vAxisTitle"]').value)
    localStorage.setItem('vMin', document.querySelector('input[name="vAxisMin"]').value)
    localStorage.setItem('vMax', document.querySelector('input[name="vAxisMax"]').value)
    localStorage.setItem('nf', document.querySelector('input[name="nf"]').checked)
    localStorage.setItem('trendline', document.querySelector('#trendline').checked)
}


const drawChart = (rows) => {
    google.charts.load('current', { 'packages': ['corechart'] })
    google.charts.setOnLoadCallback(() => {
        const data = new google.visualization.DataTable()
        const hAxis = document.querySelector('input[name="hAxisTitle"]').value
        const hAxisMin = document.querySelector('input[name="hAxisMin"]').value
        const hAxisMax = document.querySelector('input[name="hAxisMax"]').value
        const vAxis = document.querySelector('input[name="vAxisTitle"]').value
        const vAxisMin = document.querySelector('input[name="vAxisMin"]').value
        const vAxisMax = document.querySelector('input[name="vAxisMax"]').value
        const nf = document.querySelector('input[name="nf"]').checked
        const trend = document.querySelector('#trendline').checked


        data.addColumn('number', document.querySelector('input[name="hAxisTitle"').value)
        data.addColumn('number', document.querySelector('input[name="vAxisTitle"').value)

        if (rows[0] && rows[0].length === 3) {
            data.addColumn('number', 'Sem Correspondências')
        }

        rows = rows.filter(el => {
            return el[1] >= (vAxisMin ? vAxisMin : 0) && el[1] <= (vAxisMax ? vAxisMax : Number.MAX_SAFE_INTEGER)
            && el[0] >= (hAxisMin ? hAxisMin : 0) && el[0] <= (hAxisMax ? hAxisMax : Number.MAX_SAFE_INTEGER)
        })


        data.addRows(rows)


        let options = {}
        let trendline = {
            0: {
                type: '',
                color: 'red',
            }
        }

        if (!trend) {
            trendline = {}
        }

        if (!nf) {
            options = {
                title: title,
                hAxis: {
                    title: hAxis,
                    format: '0',
                    viewWindow: {
                        min: hAxisMin,
                        max: hAxisMax
                    },
                    minValue: hAxisMin,
                    maxValue: hAxisMax
                },
                vAxis: {
                    title: vAxis,
                    format: '0',
                    viewWindow: {
                        min: vAxisMin,
                        max: vAxisMax
                    },
                    minValue: vAxisMin,
                    maxValue: vAxisMax
                },
                legend: 'none',
                pointSize: 3,
                trendlines: trendline
            }

        } else {
            options = {
                title: title,
                series: {
                    0: { targetAxisIndex: 0 },
                    1: { targetAxisIndex: 1 }
                },
                vAxes: {
                    // Adds titles to each axis.
                    0: { 
                        title: vAxis,
                        format: '0'
                    },
                    1: { title: 'Sem Correspondências' }
                },
                pointSize: 3
            }
        }


        const chart = new google.visualization.ScatterChart(document.getElementById('chart_div'))
        chart.draw(data, options)
    })
}

const drawPieChart = (rows) => {

    console.log('pd', rows.pd)
    console.log('fd', rows.fd)
    console.log('nr', rows.nr)
    
    google.charts.load('current', { 'packages': ['corechart'] });
    google.charts.setOnLoadCallback(() => {
        var data = google.visualization.arrayToDataTable([
            ['Cenas', 'Cenas2'],
            ['Rec. Positivos', rows.pd],
            ['Sem Rec.', rows.nr],
            ['Rec. Falsos', rows.fd],
        ]);

        var options = {
            title: 'Reconhecimentos',
            //   pieSliceText: 'value',
            is3D: false,
        };

        var chart = new google.visualization.PieChart(document.getElementById('piechart_3d'));
        chart.draw(data, options);
    });
}

const drawHistoChart = async (rows) => {
    google.charts.load('current', { 'packages': ['corechart'] });
    google.charts.setOnLoadCallback(() => {

        let rowData = rows.map(el => {
            return ['instante', el[2] + 1]
        })

        rowData.unshift(['Instante', 'Ocorrências'])

        var data = google.visualization.arrayToDataTable(rowData)

        const hAxis = document.querySelector('input[name="hAxisTitle"]').value
        const hAxisMin = document.querySelector('input[name="hAxisMin"]').value
        const hAxisMax = document.querySelector('input[name="hAxisMax"]').value
        const vAxis = document.querySelector('input[name="vAxisTitle"]').value
        const vAxisMin = document.querySelector('input[name="vAxisMin"]').value
        const vAxisMax = document.querySelector('input[name="vAxisMax"]').value
        const nf = document.querySelector('input[name="nf"]').checked
        const trend = document.querySelector('#trendline').checked

        const options = {
            title: 'Histograma do Número de Tentativas ' + subtitle,
            hAxis: {
                title: 'Número de Ocorrências',
                format: '0',
            },
            vAxis: {
                title: 'Acumulados',
                format: '0',
            },
            legend: 'none'
        }

        const chart = new google.visualization.Histogram(document.getElementById('chart_div_histo'))
        chart.draw(data, options)
        return 1
    })
}

// Get the modal
const modal = document.getElementById("myModal");

// Get the image and insert it inside the modal - use its "alt" text as a caption
const img = document.getElementById("myImg");
const flowBtn = document.getElementById("flow");
const modalImg = document.getElementById("img01");
const captionText = document.getElementById("caption");
flowBtn.onclick = function () {
    modal.style.display = "block";
    modalImg.src = img.src;
    captionText.innerHTML = img.alt;
}

const span = document.querySelector('span')
span.addEventListener('click', () => {
    modal.style.display = 'none'
})

modal.addEventListener('click', function () {
    this.style.display = 'none' // this referes to modal
})

