const axios = require('axios')
const stats = require('statsjs')

const charts = [
    { name: 'Astart_Asend', metrics: ['ANDROID_START', 'ANDROID_SEND'] },
    { name: 'Asend_Sstart', metrics: ['ANDROID_SEND', 'SERVER_START_MATCHING'] },
    { name: 'Aresend_Srestart', metrics: ['ANDROID_RESEND', 'SERVER_RESTART_MATCHING'] },
    { name: 'Sstart_Sendmatch', metrics: ['SERVER_START_MATCHING', 'SERVER_END_MATCHING'] },
    { name: 'Srestart_Srendmatch', metrics: ['SERVER_RESTART_MATCHING', 'SERVER_REEND_MATCHING'] },
    { name: 'Snotfound_UNAorLR', metrics: ['SERVER_NOT_FOUND', 'ANDROID_USER_NOT_ABORTED', 'ANDROID_LIMIT_REACHED'] },
    { name: 'Snotfound_Aend', metrics: ['SERVER_FOUND', 'ANDROID_END'] },
    { name: 'Aend_FDorPD', metrics: ['ANDROID_END', 'ANDROID_POSITIVE_DETECTION', 'ANDROID_FALSE_DETECTION'] },
    { name: 'PD_MQTT', metrics: ['ANDROID_POSITIVE_DETECTION', 'ANDROID_MQTT_PUBLISH'] },
    { name: 'MQTT_RESP', metrics: ['ANDROID_MQTT_PUBLISH', 'HUE_DIMMER', 'HUE_TOGGLE', 'KNX_LAMP'] },
    { name: 'Astart_Aend', metrics: ['ANDROID_START', 'ANDROID_END'] },
    { name: 'Sstart_Send', metrics: ['SERVER_START_MATCHING', 'SERVER_END'] },
    { name: 'Astart_UAorLR', metrics: ['ANDROID_START', 'ANDROID_USER_ABORTED', 'ANDROID_LIMIT_REACHED'] },
    { name: 'Sstart_UAorLR', metrics: ['SERVER_START_MATCHING', 'ANDROID_USER_ABORTED', 'ANDROID_LIMIT_REACHED'] },
    { name: 'Astart_RESP', metrics: ['ANDROID_START', 'HUE_DIMMER', 'HUE_TOGGLE', 'KNX_LAMP'] }

]

const getChartData = (req) => {
    const chartType = req.body.chartType
    const chart = req.body.chart
    const time1 = new Date(req.body.time1).getTime()
    const time2 = new Date(req.body.time2).getTime()
    const nf = req.body.nf === 'on'
    const suit = req.body.suit


    return new Promise((resolve, reject) => {
        const params = {
            db: 'mydb',
            epoch: 'ms',
            time1,
            time2,
            order_by: null,
            limit: null,
            charts: true
        }

        const getData = async (params) => {
            return await axios.post('http://localhost:3000/csv', params)
        }

        getData(params).then((resp) => {

            if (resp.status !== 200) {
                throw new Error('error fetching the csv endoint')
            }

            let values = []
            let data = resp.data
            let initTimestamp
            let lastTimestamp
            let previousSuit
            let result

            if (suit) {
                data.forEach((el) => {
                    if (el.sceneDesc && el.sceneDesc.toUpperCase().includes(suit.toUpperCase())) {
                        if (!initTimestamp) {
                            initTimestamp = el.timestamp
                        }
                    } else if (initTimestamp && el.sceneDesc !== 'null' && el.sceneDesc && !lastTimestamp) {
                        lastTimestamp = previousSuit.timestamp
                    }
                    previousSuit = el
                })
                
                if(lastTimestamp === undefined){
                    lastTimestamp = previousSuit.timestamp
                }
            }

            const metricsArray = charts.filter(c => c.name === chart)
            const metric1 = metricsArray[0].metrics[0]
            const metric2 = metricsArray[0].metrics[1]
            const metric3 = metricsArray[0].metrics[2]
            const metric4 = metricsArray[0].metrics[3]

            values = data
            let feedback = data.filter(el => el.metric === 'ANDROID_POSITIVE_DETECTION' || el.metric === 'ANDROID_FALSE_DETECTION'
            || el.metric === 'ANDROID_USER_ABORTED' || el.metric === 'ANDROID_LIMIT_REACHED')
            if(suit){
                values = data.filter((el) =>  initTimestamp && lastTimestamp && el.timestamp >= initTimestamp && el.timestamp <= lastTimestamp)
                feedback = feedback.filter((el) =>  initTimestamp && lastTimestamp && el.timestamp >= initTimestamp && el.timestamp <= lastTimestamp)
            }
            result = values.filter((el) => {
                const m3Cond = metric3 ? el.metric === metric3 : undefined
                const m4Cond = metric4 ? el.metric === metric4 : undefined
                return el.metric === metric1 || el.metric === metric2 || m3Cond || m4Cond
            }).map(el => {
                return { timestamp: el.timestamp, metric: el.metric, nf: el.nf}
            })

            prepareValues(result, nf, feedback, metric1, metric2, metric3, metric4).then(data => {
                resolve(data)
            }).catch(err => {
                console.log(err)
            })
        })
    })
}

const prepareValues = async (result, nf, feedback, ...metric) => {
    let data = []
    let counter = 0
    let index = 0
    let previousValue
    result.forEach(v => {
        const metric3Cond = metric[2] ? v.metric === metric[2] : undefined
        const metric4Cond = metric[3] ? v.metric === metric[3] : undefined
        if (v.metric === metric[0]) {
            previousValue = v
        } else if (previousValue.timestamp === result[counter - 1].timestamp && (v.metric === metric[1] || metric3Cond || metric4Cond)) {
            if (nf) {
                data.push([index++, v.timestamp - previousValue.timestamp, parseInt(v.nf, 10)])
            } else {
                data.push([index++, v.timestamp - previousValue.timestamp])
            }
        }
        counter++
    })

    const table = getTableData(data, feedback)
    if(nf){
        data = data.filter(el => el[2] > 0)
    }
    return {
        data,
        table
    }

}

const getTableData = (data, feedback) => {
    const result = {}

    let dataArray = []
    data.forEach(el => {
        dataArray.push(el[1])
    })

    const statsData = stats(dataArray)

    result.total = statsData.size()
    result.min = statsData.min()
    result.q1 = statsData.q1()
    result.q2 = statsData.median()
    result.q3 = statsData.q3()
    result.max = statsData.max()
    result.average = statsData.mean()
    result.stdDev = statsData.stdDev()
    const outliers = getOutliers(result.q1, result.q3, dataArray)
    result.outliers = outliers.outliers
    result.inf = outliers.inf
    result.sup = outliers.sup
    result.outliersPerc = result.outliers / result.total * 100
    result.nf = data.map(el => el[2]).reduce((acc, curr) =>  acc + curr, 0)
    result.pd = feedback.filter(el => el.metric === 'ANDROID_POSITIVE_DETECTION').map(el => 1).reduce((acc, curr) => acc + curr, 0)
    result.fd = feedback.filter(el => el.metric === 'ANDROID_FALSE_DETECTION').map(el => 1).reduce((acc, curr) => acc + curr, 0)
    result.nr = feedback.filter(el => el.metric === 'ANDROID_USER_ABORTED' || el.metric === 'ANDROID_LIMIT_REACHED').map(el => 1).reduce((acc, curr) => acc + curr, 0)
    return result
}

const getOutliers = (q1, q3, dataArray) => {
    const iqr = q3 - q1
    const sup = q3 + 1.5 * iqr
    const inf = q1 - 1.5 * iqr
    let outliers = 0
    dataArray.forEach(el => {
        if(el > sup || el < inf){
            outliers++
        }
    })
    return {
        outliers,
        inf,
        sup
    }
}

module.exports = {
    getChartData
}