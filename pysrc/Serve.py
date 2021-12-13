#!/usr/bin/env python3
import asyncio
import sys

import uvicorn
from fastapi import FastAPI
from starlette.staticfiles import StaticFiles
import GloveMap

app = FastAPI(
    title=__name__,
    description='This is the API for traveler-integrated',
    version='0.1.1'
)
# app.mount('/static', StaticFiles(directory='static'), name='static')

app.include_router(GloveMap.router)

if __name__ == '__main__':
    print('Serving on localhost')
    uvicorn.run(app, host='0.0.0.0', port=8080, log_level='warning')