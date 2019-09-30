const axios = require('axios')

let enterTime
let db
let epoch
let time1
let time2
let orderBy
let limit
let charts

let values
let csv
let queries = []

const buildParams = (tag) => {
    let query = `SELECT * FROM ${tag}`

    time1 ? query += ` WHERE time >= ${time1}ms` : undefined
    time2 ? query += ` AND time <= ${time2}ms` : undefined
    orderBy ? query += ` ORDER BY ${orderBy} ASC` : undefined
    limit ? query += ` LIMIT ${limit}` : undefined

    queries.push(query)

    return {
        db: db,
        epoch: epoch,
        q: query
    }
}

const compare = (a, b) => {
    return a.timestamp < b.timestamp ? -1 : a.timestamp > b.timestamp ? 1 : 0
}

const push2Values = (el, metric) => {
    if (!values) {
        values = []
    }
    const obj = {}
    if (el.length === 2) {
        obj.timestamp = el[0]
        obj.deltat = ''
        obj.objDesc = ''
        obj.nf = ''
        obj.sceneDesc = ''
        obj.metric = metric
        obj.value = el[1]
    }
    else {
        obj.timestamp = el[0]
        obj.deltat = ''
        obj.objDesc = el[1]
        obj.nf = el[2]
        obj.sceneDesc = el[3]
        obj.metric = el[4]
        obj.value = el[5]
    }
    values.push(obj)
}

// promise
const query = (params) => axios.get('http://influxdb.joaobrito.local:8086/query', { params })

const populateValues = () => {
    values = undefined
    const knxParams = buildParams('lamp')
    const light1DimerParams = buildParams('Light1_Dimmer')
    const light1ToggleParams = buildParams('Light1_Toggle')
    const androidParams = buildParams('android')
    const serverParams = buildParams('server')
    const tomaParams = buildParams('toma')
    const ventParams = buildParams('vent')

    return axios.all([
        query(knxParams),
        query(light1DimerParams),
        query(light1ToggleParams),
        query(androidParams),
        query(serverParams),
        query(tomaParams),
        query(ventParams)
    ]).then(axios.spread((lamp, dimm, toggle, and, serv, toma, vent) => {
        if (lamp.data.results[0].series) {
            lamp.data.results[0].series[0].values.forEach(el => {
                push2Values(el, 'KNX_LAMP')
            })
        }
        if (dimm.data.results[0].series) {
            dimm.data.results[0].series[0].values.forEach(el => {
                push2Values(el, 'HUE_DIMMER')
            })
        }
        if (toggle.data.results[0].series) {
            toggle.data.results[0].series[0].values.forEach(el => {
                push2Values(el, 'HUE_TOGGLE')
            })
        }
        if (and.data.results[0].series) {
            and.data.results[0].series[0].values.forEach(el => {
                push2Values(el)
            })
        }
        if (serv.data.results[0].series) {
            serv.data.results[0].series[0].values.forEach(el => {
                push2Values(el)
            })
        }
        if (toma.data.results[0].series) {
            toma.data.results[0].series[0].values.forEach(el => {
                push2Values(el, 'TOMADA')
            })
        }
        if (vent.data.results[0].series) {
            vent.data.results[0].series[0].values.forEach(el => {
                push2Values(el, 'VENTOINHA')
            })
        }
    })).catch((error) => {
        throw error
    })
}

const init = () => {
    enterTime = undefined
    db = undefined
    epoch = undefined
    time1 = undefined
    time2 = undefined
    orderBy = undefined
    limit = undefined
    charts = undefined

    values = undefined
    csv = undefined
    queries = []
}

// asynchronous function
const getDataAsync = async (req) => {

    init()

    // get body values
    enterTime = new Date().getTime();
    db = req.body.db
    epoch = req.body.epoch
    time1 = req.body.time1
    time2 = req.body.time2
    orderBy = req.body.order_by
    limit = req.body.limit
    charts = req.body.charts


    await populateValues().catch((err) => {
        throw err
    })

    let counter = 0
    if (values) {
        values.sort(compare)
        values.forEach((el) => {
            el.id = ++counter
        })
        values.unshift({
            id: 'ID',
            timestamp: 'TIMESTAMP',
            deltat: 'DELTA T',
            objDesc: 'OBJECT',
            nf: 'NF',
            sceneDesc: 'SCENE',
            metric: 'METRIC',
            value: 'VAL'
        })
        csv = values.map(val =>  `${val.id},${val.timestamp},${val.deltat},${val.objDesc},${val.nf},${val.sceneDesc},${val.metric},${val.value}`).join('\n')

        return {
            csv: csv,
            count: values.length - 1,
            queries: queries,
            time: new Date().getTime() - enterTime,
            json : charts ? values : undefined
        }
    } else {
        throw '404 - No data was found!';
    }
}



module.exports = {
    getDataAsync
}

