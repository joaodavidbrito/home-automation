'use strict'

const express = require('express')
const bodyParser = require("body-parser")
const hbs = require('hbs')
const path = require('path')
const { getDataAsync } = require('./assets/js/scripts/csv')
const { getChartData } = require('./assets/js/scripts/charts')

// Constants
const PORT = process.env.PORT || 3000

// App
const app = express()

// body parser
app.use(bodyParser.json())
app.use(bodyParser.urlencoded({ extended: true }))

// statics
app.use(express.static(path.resolve(__dirname, 'public/assets')))
app.use(express.static(path.resolve(__dirname, '')))


// hbs settings
app.set('view engine', 'html')
app.engine('html', hbs.__express)
hbs.registerPartials(path.resolve(__dirname, 'public/views/partials'))

// root (default) endpoint
app.get('/', (req, res) => {
    res.status(200).render('index.html')
})

// csv endpoint
app.post('/csv', (req, res) => {
    getDataAsync(req).then(({ csv, count, queries, time, json }) => {
        if (!json) {
            res.status(200).render('done.html', { csv, count, queries, time })
        } else {
            res.status(200).send(json)
        }
    }).catch((error) => {
        res.status(404).render('error.html', { error })
    })
})

// charts endpoint
app.get('/charts', (req, res) => {
    res.status(200).render('charts.html')
})

// fetch charts data from /charts
app.post('/charts_fetch_data', (req, res) => {
    getChartData(req).then((data) => {
        res.status(200).send(data)
    }).catch((err) => {
        res.status(404).render('error.html', { err })
    })
})

// back endpoint: redirects to root: /
app.get('/back', (req, res) => {
    res.redirect('/')
})

// default get - 404
app.get('/*', (req, res) => {
    res.status(404)
        .render('404.html')
})

// create the server and listen on the specified port
app.listen(PORT, () => {
    console.log(`Running started and listening on port: ${PORT}`)
})